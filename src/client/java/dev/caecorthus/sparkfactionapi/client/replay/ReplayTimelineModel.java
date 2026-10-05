package dev.caecorthus.sparkfactionapi.client.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Filter state of the replay timeline: one category (null = all), one player, and a search query. Results are
 * recomputed only when a filter changes; rendering reads them without allocating.
 * Search is case-insensitive, splits the query on whitespace, and requires every term to appear in the line text
 * (legacy {@code §} codes stripped) or in the name of a player the line involves.
 * 回放时间线的筛选状态：一个分类（null 为全部）、一名玩家与一个搜索词。只在筛选变化时重算，渲染时读取结果不分配内存。
 * 搜索不区分大小写，按空白拆分为多个词，每个词都须出现在行文本（已去除 {@code §} 格式码）或该行涉及玩家的名字中。
 */
final class ReplayTimelineModel {
    private static final ReplaySnapshot.Category[] CATEGORIES = ReplaySnapshot.Category.values();

    private final List<ReplaySnapshot.Line> lines;
    private final Map<UUID, ReplaySnapshot.Player> players;
    private final String[] haystacks;
    private final int[] totals = new int[CATEGORIES.length];
    private final int[] counts = new int[CATEGORIES.length];
    private int[] visible = new int[0];
    private int visibleCount;
    private int matchingCount;
    private @Nullable ReplaySnapshot.Category category;
    private @Nullable UUID playerFilter;
    private String query = "";
    private String[] terms = new String[0];
    private int version;

    ReplayTimelineModel(ReplaySnapshot snapshot) {
        this.lines = snapshot.lines();
        this.players = new HashMap<>();
        for (ReplaySnapshot.Player player : snapshot.players()) {
            players.putIfAbsent(player.uuid(), player);
        }
        this.haystacks = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            ReplaySnapshot.Line line = lines.get(i);
            totals[line.category().ordinal()]++;
            StringBuilder text = new StringBuilder(stripped(line.text().getString()));
            for (UUID uuid : line.players()) {
                ReplaySnapshot.Player player = players.get(uuid);
                if (player != null) {
                    text.append('\n').append(player.name());
                }
            }
            haystacks[i] = text.toString().toLowerCase(Locale.ROOT);
        }
        this.visible = new int[lines.size()];
        recompute();
    }

    // ---- filters / 筛选

    @Nullable ReplaySnapshot.Category category() {
        return category;
    }

    void setCategory(@Nullable ReplaySnapshot.Category category) {
        if (this.category != category) {
            this.category = category;
            recompute();
        }
    }

    @Nullable UUID playerFilter() {
        return playerFilter;
    }

    void setPlayerFilter(@Nullable UUID uuid) {
        if (!java.util.Objects.equals(playerFilter, uuid)) {
            playerFilter = uuid;
            recompute();
        }
    }

    /** Selects the player, or clears the filter when that player is already selected. 选中该玩家；已选中时清除筛选。 */
    void togglePlayerFilter(UUID uuid) {
        setPlayerFilter(uuid.equals(playerFilter) ? null : uuid);
    }

    String query() {
        return query;
    }

    void setQuery(@Nullable String raw) {
        String next = raw == null ? "" : raw;
        if (next.equals(query)) {
            return;
        }
        query = next;
        String normalized = next.trim().toLowerCase(Locale.ROOT);
        terms = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
        recompute();
    }

    /** Bumped on every effective filter change, so views can tell when to rebuild. 每次筛选实际变化时递增，供视图判断是否重建。 */
    int version() {
        return version;
    }

    // ---- results / 结果

    int total() {
        return lines.size();
    }

    int visibleCount() {
        return visibleCount;
    }

    /** Snapshot line index of the i-th visible line, ascending. 第 i 条可见行在快照中的下标，升序。 */
    int visibleLine(int position) {
        if (position < 0 || position >= visibleCount) {
            throw new IndexOutOfBoundsException(position);
        }
        return visible[position];
    }

    /** Position of a snapshot line in the visible list, or -1. 快照行在可见列表中的位置；不可见时为 -1。 */
    int visiblePosition(int lineIndex) {
        int found = Arrays.binarySearch(visible, 0, visibleCount, lineIndex);
        return found >= 0 ? found : -1;
    }

    /**
     * Lines of {@code category} (null = every category) that pass the player and search filters; the category filter
     * itself is ignored so tab counts preview what clicking the tab would show.
     * 通过玩家与搜索筛选的某分类行数（null 为所有分类）；不考虑分类筛选本身，因此标签计数即点击该标签后会看到的数量。
     */
    int count(@Nullable ReplaySnapshot.Category category) {
        return category == null ? matchingCount : counts[category.ordinal()];
    }

    /** Lines of {@code category} in the whole match (null = all lines). 整局中该分类的行数（null 为全部）。 */
    int totalCount(@Nullable ReplaySnapshot.Category category) {
        return category == null ? lines.size() : totals[category.ordinal()];
    }

    @Nullable ReplaySnapshot.Player player(@Nullable UUID uuid) {
        return uuid == null ? null : players.get(uuid);
    }

    ReplaySnapshot.Line line(int lineIndex) {
        return lines.get(lineIndex);
    }

    // ---- internals / 内部

    private void recompute() {
        Arrays.fill(counts, 0);
        int shown = 0;
        int matching = 0;
        for (int i = 0; i < lines.size(); i++) {
            ReplaySnapshot.Line line = lines.get(i);
            if (!passesPlayerAndSearch(i, line)) {
                continue;
            }
            matching++;
            counts[line.category().ordinal()]++;
            if (category == null || line.category() == category) {
                visible[shown++] = i;
            }
        }
        visibleCount = shown;
        matchingCount = matching;
        version++;
    }

    private boolean passesPlayerAndSearch(int index, ReplaySnapshot.Line line) {
        if (playerFilter != null && !line.players().contains(playerFilter)) {
            return false;
        }
        String haystack = haystacks[index];
        for (String term : terms) {
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static String stripped(String text) {
        String plain = Formatting.strip(text);
        return plain == null ? "" : plain;
    }
}
