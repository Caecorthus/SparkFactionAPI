package dev.caecorthus.sparkfactionapi.impl.record;

/**
 * {@code kind} of a {@code sparkfactionapi:consume} record; see {@link ConsumeRules#classify}.
 * {@code sparkfactionapi:consume} 记录的 {@code kind}；见 {@link ConsumeRules#classify}。
 */
public enum ConsumeKind {
    FOOD("food"),
    DRINK("drink");

    private final String id;

    ConsumeKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
