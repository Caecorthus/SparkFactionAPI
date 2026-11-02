package dev.caecorthus.sparkfactionapi.impl.team;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps {@link CaecorthusTeamTag}'s member list in step with the Caecorthus website. Client and server each fetch
 * {@code GET /api/team/minecraft} on a background thread at start-up and every 30 minutes. A failed fetch keeps the
 * last list, and the last good response is cached in the config folder so an offline start still shows the badges.
 * 让 {@link CaecorthusTeamTag} 的名单跟网站一致。客户端和服务端各自在后台线程拉取 {@code GET /api/team/minecraft}：启动时一次，
 * 之后每 30 分钟一次。拉取失败时沿用上一次的名单；最后一次成功的回答存在 config 文件夹里，离线启动也有标签。
 */
public final class CaecorthusTeamSync {
    static final URI ENDPOINT = URI.create("https://express-dlc.tlspark.cn/api/team/minecraft");
    private static final long REFRESH_MINUTES = 30;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static boolean started;
    private static String lastBody;
    private static boolean failing;

    private CaecorthusTeamSync() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        Path cache = FabricLoader.getInstance().getConfigDir()
                .resolve(SparkFactionApiMod.MOD_ID)
                .resolve("caecorthus_team.json");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "SparkFactionAPI team list");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.execute(() -> loadCache(cache));
        scheduler.scheduleWithFixedDelay(() -> refresh(client, cache), 0, REFRESH_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Parses the website's {@code {"members": [{"uuid": "..."}]}}. Empty when the shape is wrong, so a broken
     * response never wipes the list; entries without a valid UUID are skipped.
     * 解析网站的回答。格式不对时返回空，免得一次坏回答把名单清掉；没有有效 UUID 的条目跳过。
     */
    static Optional<Set<UUID>> parseMembers(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject() || !(root.getAsJsonObject().get("members") instanceof JsonArray entries)) {
                return Optional.empty();
            }
            Set<UUID> members = new HashSet<>();
            for (JsonElement entry : entries) {
                JsonElement uuid = entry instanceof JsonObject member ? member.get("uuid") : null;
                if (uuid != null && uuid.isJsonPrimitive() && uuid.getAsJsonPrimitive().isString()) {
                    try {
                        members.add(UUID.fromString(uuid.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                        // skip a malformed UUID / 跳过格式不对的 UUID
                    }
                }
            }
            return Optional.of(Set.copyOf(members));
        } catch (JsonParseException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    private static void loadCache(Path cache) {
        try {
            if (Files.isRegularFile(cache)) {
                String body = Files.readString(cache, StandardCharsets.UTF_8);
                parseMembers(body).ifPresent(members -> {
                    CaecorthusTeamTag.setMembers(members);
                    lastBody = body;
                });
            }
        } catch (IOException | RuntimeException e) {
            SparkFactionApiMod.LOGGER.debug("Could not read the cached Caecorthus team list", e);
        }
    }

    // Runs on the scheduler thread; never throws, or the scheduled refresh would stop for good.
    // 在定时线程上跑，不能抛出异常，否则之后的定时刷新就停了。
    private static void refresh(HttpClient client, Path cache) {
        try {
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", "SparkFactionAPI/" + modVersion())
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                fail("HTTP " + response.statusCode());
                return;
            }
            String body = response.body();
            Optional<Set<UUID>> members = parseMembers(body);
            if (members.isEmpty()) {
                fail("unexpected response");
                return;
            }
            CaecorthusTeamTag.setMembers(members.get());
            if (!body.equals(lastBody)) {
                writeCache(cache, body);
                lastBody = body;
            }
            if (failing) {
                SparkFactionApiMod.LOGGER.info("Caecorthus team list is reachable again ({} members)", members.get().size());
                failing = false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            fail(e.toString());
        }
    }

    private static void writeCache(Path cache, String body) {
        try {
            Files.createDirectories(cache.getParent());
            Path temp = cache.resolveSibling(cache.getFileName() + ".tmp");
            Files.writeString(temp, body, StandardCharsets.UTF_8);
            Files.move(temp, cache, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            SparkFactionApiMod.LOGGER.debug("Could not cache the Caecorthus team list", e);
        }
    }

    // Warn once per outage, so an offline client doesn't log every 30 minutes. / 每次断线只警告一次。
    private static void fail(String reason) {
        if (!failing) {
            SparkFactionApiMod.LOGGER.warn("Could not refresh the Caecorthus team list from {}: {}", ENDPOINT, reason);
            failing = true;
        }
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(SparkFactionApiMod.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
