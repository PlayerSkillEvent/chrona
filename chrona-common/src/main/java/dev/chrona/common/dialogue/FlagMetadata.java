package dev.chrona.common.dialogue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Zusätzliche Metadaten für ein Flag-Set/Unset.
 *
 * - source: Woher kommt die Änderung? (z.B. "dialogue:ARENKHET_INTRO", "quest:ARC-PH1-01", "cmd:/setflag")
 * - world, x, y, z: Ort im Spiel (optional)
 * - extra: freie Zusatzinfos (npcId, questId, choiceId, etc.)
 */
public final class FlagMetadata {

    private final String source;
    private final Map<String, Object> extra;

    private FlagMetadata(Builder builder) {
        this.source = builder.source;
        this.extra = builder.extra != null
                ? Map.copyOf(builder.extra)
                : Collections.emptyMap();
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    // Convenience: leere Metadata
    public static FlagMetadata empty() {
        return new Builder().build();
    }

    // Convenience: nur Source
    public static FlagMetadata ofSource(String source) {
        return new Builder().source(source).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String source;
        private Map<String, Object> extra;

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder extra(Map<String, Object> extra) {
            this.extra = extra;
            return this;
        }

        public Builder putExtra(String key, Object value) {
            if (this.extra == null) {
                this.extra = new HashMap<>();
            }
            this.extra.put(key, value);
            return this;
        }

        public FlagMetadata build() {
            return new FlagMetadata(this);
        }
    }
}
