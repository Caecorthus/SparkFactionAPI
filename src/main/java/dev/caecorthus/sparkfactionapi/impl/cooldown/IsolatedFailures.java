package dev.caecorthus.sparkfactionapi.impl.cooldown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logs a failing downstream callback (store, provider, exemption) without letting it break other integrations.
 * The first failure per source is a WARN with the stack trace; repeats drop to DEBUG so per-tick callers
 * (auras) cannot flood the log.
 * 记录下游回调（存储、提供者、豁免）的异常，但不让其影响其他集成。每个来源首次失败以 WARN 带堆栈输出，之后降为 DEBUG，
 * 防止每 tick 调用的光环刷屏。
 */
final class IsolatedFailures {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/ForcedCooldowns");

    private final Set<String> warnedSources = ConcurrentHashMap.newKeySet();

    void report(String source, String outcome, Throwable failure) {
        if (warnedSources.add(source)) {
            LOGGER.warn("{} threw; {} (further failures from this source are logged at debug)", source, outcome, failure);
        } else {
            LOGGER.debug("{} threw again; {}", source, outcome, failure);
        }
    }
}
