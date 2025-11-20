package dev.chrona.quest.model;

/**
 * Represents the timing configuration for a quest, including expiration and cooldown settings.
 */
public final class QuestTiming {

    private final Long expiresAfterSeconds;
    private final Long cooldownSeconds;
    private final boolean hardFailOnExpire;

    public QuestTiming(Long expiresAfterSeconds, Long cooldownSeconds, boolean hardFailOnExpire) {
        this.expiresAfterSeconds = expiresAfterSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.hardFailOnExpire = hardFailOnExpire;
    }

    /**
     * Gets the expiration time in seconds.
     *
     * @return the expiration time in seconds
     */
    public Long expiresAfterSeconds() {
        return expiresAfterSeconds;
    }

    /**
     * Gets the cooldown time in seconds.
     *
     * @return the cooldown time in seconds
     */
    public Long cooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Indicates whether the quest should hard fail upon expiration.
     *
     * @return true if the quest hard fails on expiration, false otherwise
     */
    public boolean hardFailOnExpire() {
        return hardFailOnExpire;
    }
}
