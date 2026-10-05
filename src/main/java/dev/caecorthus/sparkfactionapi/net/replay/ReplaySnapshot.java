package dev.caecorthus.sparkfactionapi.net.replay;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Frozen, display-ready replay of one finished match, sent to clients that open the replay screen.
 * Texts are server-built translatable components so each client renders them in its own language. Player names
 * inside {@link Line#text()} carry a {@link ReplayTextTags} insertion instead of a hover payload; the client resolves
 * the tagged UUID against {@link #players()} so each tooltip travels once.
 * 单局已结束对局的冻结回放，可直接展示，发送给打开回放界面的客户端。文本均为服务端构建的可翻译组件，由客户端按自身语言渲染。
 * {@link Line#text()} 中的玩家名不携带悬停内容，而是带 {@link ReplayTextTags} 插入标记；客户端按 UUID 到
 * {@link #players()} 中查找，使每份悬停提示只传输一次。
 */
public record ReplaySnapshot(
        UUID matchId,
        Identifier gameMode,
        Identifier mapEffect,
        int durationSeconds,
        int endedSecondsAgo,
        Optional<Text> outcome,
        List<Player> players,
        List<Line> lines
) {
    private static final int MAX_PLAYERS = 256;
    private static final int MAX_LINES = 8192;
    private static final int MAX_ROLES = 64;
    private static final int MAX_BADGES = 32;
    private static final int MAX_LINE_PLAYERS = 16;

    public static final PacketCodec<RegistryByteBuf, ReplaySnapshot> CODEC =
            PacketCodec.of(ReplaySnapshot::write, ReplaySnapshot::read);

    public ReplaySnapshot {
        players = List.copyOf(players);
        lines = List.copyOf(lines);
    }

    /** Same snapshot with a new "ended N seconds ago" value, stamped at send time. 以发送时刻重算“结束于多久前”的副本。 */
    public ReplaySnapshot withEndedSecondsAgo(int secondsAgo) {
        return new ReplaySnapshot(matchId, gameMode, mapEffect, durationSeconds, secondsAgo, outcome, players, lines);
    }

    public @Nullable Player player(UUID uuid) {
        for (Player player : players) {
            if (player.uuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    public enum Presence {
        ALIVE,
        DEAD,
        LEFT
    }

    public enum Result {
        WIN,
        LOSE,
        NONE
    }

    /**
     * Timeline filter buckets. Unknown add-on event types fall into {@link #OTHER}.
     * 时间线筛选分类；未知的附属模组事件类型归入 {@link #OTHER}。
     */
    public enum Category {
        DEATH,
        CONVERSION,
        SKILL,
        SHOP,
        ITEM,
        OTHER
    }

    /**
     * One participant. {@code roles} is never empty: the first step is the opening role.
     * 单名参与者。{@code roles} 不为空，首项为开局身份。
     */
    public record Player(
            UUID uuid,
            String name,
            List<RoleStep> roles,
            Text faction,
            Presence presence,
            int deathSecond,
            Result result,
            Text tooltip,
            List<Badge> badges
    ) {
        public Player {
            roles = List.copyOf(roles);
            badges = List.copyOf(badges);
        }

        public RoleStep finalRole() {
            return roles.getLast();
        }
    }

    /**
     * A role held by a player; {@code reachedSecond} is -1 for the opening role.
     * 玩家持有过的身份；开局身份的 {@code reachedSecond} 为 -1。
     */
    public record RoleStep(Identifier role, Text label, int color, int reachedSecond) {
    }

    /** A small coloured label in the roster card, e.g. one trait. 名单卡片中的彩色小标签，例如一个词条。 */
    public record Badge(Text label, int color, Optional<Text> tooltip) {
    }

    /**
     * One timeline line without its time prefix; {@code players} lists everyone the event involves.
     * 一条不带时间前缀的时间线记录；{@code players} 为该事件涉及的所有玩家。
     */
    public record Line(int second, Category category, List<UUID> players, Text text) {
        public Line {
            players = List.copyOf(players);
        }
    }

    private static void write(ReplaySnapshot snapshot, RegistryByteBuf buf) {
        buf.writeUuid(snapshot.matchId);
        buf.writeIdentifier(snapshot.gameMode);
        buf.writeIdentifier(snapshot.mapEffect);
        buf.writeVarInt(snapshot.durationSeconds);
        buf.writeVarInt(snapshot.endedSecondsAgo);
        writeOptionalText(buf, snapshot.outcome);
        writeCount(buf, snapshot.players.size(), MAX_PLAYERS);
        for (Player player : snapshot.players) {
            buf.writeUuid(player.uuid);
            buf.writeString(player.name, 64);
            writeCount(buf, player.roles.size(), MAX_ROLES);
            for (RoleStep role : player.roles) {
                buf.writeIdentifier(role.role);
                TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, role.label);
                buf.writeInt(role.color);
                buf.writeVarInt(role.reachedSecond + 1);
            }
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, player.faction);
            buf.writeEnumConstant(player.presence);
            buf.writeVarInt(player.deathSecond + 1);
            buf.writeEnumConstant(player.result);
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, player.tooltip);
            writeCount(buf, player.badges.size(), MAX_BADGES);
            for (Badge badge : player.badges) {
                TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, badge.label);
                buf.writeInt(badge.color);
                writeOptionalText(buf, badge.tooltip);
            }
        }
        writeCount(buf, snapshot.lines.size(), MAX_LINES);
        for (Line line : snapshot.lines) {
            buf.writeVarInt(line.second);
            buf.writeEnumConstant(line.category);
            writeCount(buf, line.players.size(), MAX_LINE_PLAYERS);
            for (UUID uuid : line.players) {
                buf.writeUuid(uuid);
            }
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, line.text);
        }
    }

    private static ReplaySnapshot read(RegistryByteBuf buf) {
        UUID matchId = buf.readUuid();
        Identifier gameMode = buf.readIdentifier();
        Identifier mapEffect = buf.readIdentifier();
        int durationSeconds = buf.readVarInt();
        int endedSecondsAgo = buf.readVarInt();
        Optional<Text> outcome = readOptionalText(buf);
        int playerCount = readCount(buf, MAX_PLAYERS);
        List<Player> players = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString(64);
            int roleCount = readCount(buf, MAX_ROLES);
            List<RoleStep> roles = new ArrayList<>(roleCount);
            for (int r = 0; r < roleCount; r++) {
                roles.add(new RoleStep(
                        buf.readIdentifier(),
                        TextCodecs.REGISTRY_PACKET_CODEC.decode(buf),
                        buf.readInt(),
                        buf.readVarInt() - 1
                ));
            }
            Text faction = TextCodecs.REGISTRY_PACKET_CODEC.decode(buf);
            Presence presence = buf.readEnumConstant(Presence.class);
            int deathSecond = buf.readVarInt() - 1;
            Result result = buf.readEnumConstant(Result.class);
            Text tooltip = TextCodecs.REGISTRY_PACKET_CODEC.decode(buf);
            int badgeCount = readCount(buf, MAX_BADGES);
            List<Badge> badges = new ArrayList<>(badgeCount);
            for (int b = 0; b < badgeCount; b++) {
                badges.add(new Badge(TextCodecs.REGISTRY_PACKET_CODEC.decode(buf), buf.readInt(), readOptionalText(buf)));
            }
            if (roles.isEmpty()) {
                throw new IllegalArgumentException("Replay player without roles: " + uuid);
            }
            players.add(new Player(uuid, name, roles, faction, presence, deathSecond, result, tooltip, badges));
        }
        int lineCount = readCount(buf, MAX_LINES);
        List<Line> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            int second = buf.readVarInt();
            Category category = buf.readEnumConstant(Category.class);
            int involvedCount = readCount(buf, MAX_LINE_PLAYERS);
            List<UUID> involved = new ArrayList<>(involvedCount);
            for (int p = 0; p < involvedCount; p++) {
                involved.add(buf.readUuid());
            }
            lines.add(new Line(second, category, involved, TextCodecs.REGISTRY_PACKET_CODEC.decode(buf)));
        }
        return new ReplaySnapshot(matchId, gameMode, mapEffect, durationSeconds, endedSecondsAgo, outcome, players, lines);
    }

    private static void writeOptionalText(RegistryByteBuf buf, Optional<Text> text) {
        buf.writeBoolean(text.isPresent());
        text.ifPresent(value -> TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, value));
    }

    private static Optional<Text> readOptionalText(RegistryByteBuf buf) {
        return buf.readBoolean() ? Optional.of(TextCodecs.REGISTRY_PACKET_CODEC.decode(buf)) : Optional.empty();
    }

    private static void writeCount(RegistryByteBuf buf, int count, int max) {
        if (count > max) {
            throw new IllegalArgumentException("Replay snapshot list too long: " + count + " > " + max);
        }
        buf.writeVarInt(count);
    }

    private static int readCount(RegistryByteBuf buf, int max) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) {
            throw new IllegalArgumentException("Replay snapshot list length out of range: " + count);
        }
        return count;
    }
}
