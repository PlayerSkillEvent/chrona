package dev.chrona.quest.engine;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Contextual information for quest-related events and operations.
 */
public final class QuestContext {

    private final Player player;
    private final Location location;
    private final String npcId;
    private final String regionId;
    private final String reason;
    private final Map<String, Object> extra;

    private QuestContext(Builder b) {
        this.player = b.player;
        this.location = b.location;
        this.npcId = b.npcId;
        this.regionId = b.regionId;
        this.reason = b.reason;
        this.extra = b.extra != null ? Map.copyOf(b.extra) : Map.of();
    }

    /** Returns the player associated with this context. */
    public Player player() { return player; }

    /** Returns the location associated with this context. */
    public Location location() { return location; }

    /** Returns the NPC ID associated with this context. */
    public String npcId() { return npcId; }

    /** Returns the region ID associated with this context. */
    public String regionId() { return regionId; }

    /** Returns the reason for this context. */
    public String reason() { return reason; }

    /** Returns additional contextual data as an unmodifiable map. */
    public Map<String, Object> extra() { return extra; }

    /** Creates a new builder for QuestContext. */
    public static Builder builder() { return new Builder(); }

    /** Builder class for constructing QuestContext instances. */
    public static final class Builder {
        private Player player;
        private Location location;
        private String npcId;
        private String regionId;
        private String reason;
        private Map<String, Object> extra;

        /** Sets the player for the context. */
        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        /** Sets the location for the context. */
        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        /** Sets the NPC ID for the context. */
        public Builder npcId(String npcId) {
            this.npcId = npcId;
            return this;
        }

        /** Sets the region ID for the context. */
        public Builder regionId(String regionId) {
            this.regionId = regionId;
            return this;
        }

        /** Sets the reason for the context. */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /** Adds an extra key-value pair to the context. */
        public Builder putExtra(String key, Object value) {
            if (this.extra == null)
                this.extra = new HashMap<>();
            this.extra.put(key, value);
            return this;
        }

        /** Builds the QuestContext instance. */
        public QuestContext build() {
            return new QuestContext(this);
        }
    }
}
