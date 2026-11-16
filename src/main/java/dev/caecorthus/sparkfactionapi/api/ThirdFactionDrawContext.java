package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * What a {@link ThirdFactionLineup} sees during the third-faction draw.
 * {@link ThirdFactionLineup} 在第三方阵营抽取时看到的信息。
 *
 * @param players        every player of the round (the list Wathe assigns roles to), unmodifiable
 *                       本局所有玩家（Wathe 分配角色的列表），不可修改
 * @param totalPlayers   {@code players.size()}
 * @param memberCap      {@code floor(totalPlayers / thirdFactionDividend)}, at least 1 whenever a lineup is asked
 *                       {@code floor(总人数 / 第三方阵营除数)}，询问阵容时至少为 1
 * @param factionId      the third faction being asked 被询问的第三方阵营
 * @param presentMembers roles of this faction already held when the draw runs (admin-forced roles and anything an
 *                       earlier pass dealt), in player order; empty during the eligibility probe of a faction with no
 *                       present member; unmodifiable
 *                       抽取时本阵营已被持有的角色（管理员强制角色及更早分配阶段发放的角色），按玩家顺序；对没有现有
 *                       成员的阵营做资格探测时为空；不可修改
 * @since 2.0.0
 */
public record ThirdFactionDrawContext(
        ServerWorld world,
        GameWorldComponent gameComponent,
        List<ServerPlayerEntity> players,
        int totalPlayers,
        int memberCap,
        Identifier factionId,
        List<Role> presentMembers
) {
    public ThirdFactionDrawContext {
        players = List.copyOf(players);
        Objects.requireNonNull(factionId, "factionId");
        presentMembers = List.copyOf(presentMembers);
    }

    /**
     * Slots this lineup may still fill: {@code max(0, memberCap - presentMembers.size())}.
     * 本阵容还能填补的名额：{@code max(0, memberCap - presentMembers.size())}。
     */
    public int remainingSlots() {
        return Math.max(0, memberCap - presentMembers.size());
    }
}
