package dev.chrona.common.dialogue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata associated with a flag-update in the dialogue system.
 * Includes the source of the update and any additional extra metadata.
 * This class is immutable and can be constructed using the Builder pattern.
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

    /** Returns the source of the flag-update. */
    public String getSource() {
        return source;
    }

    /** Returns additional extra metadata. */
    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Returns an empty FlagMetadata instance for convenience. */
    public static FlagMetadata empty() {
        return new Builder().build();
    }

    /** Returns a FlagMetadata instance with only the source set for convenience. */
    public static FlagMetadata ofSource(String source) {
        return new Builder().source(source).build();
    }

    /** Creates a new Builder for FlagMetadata. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder class for FlagMetadata. */
    public static final class Builder {
        private String source;
        private Map<String, Object> extra;

        /** Sets the source of the flag-update. */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /** Sets the extra metadata map. */
        public Builder extra(Map<String, Object> extra) {
            this.extra = extra;
            return this;
        }

        /** Puts a single key-value pair into the extra metadata map. */
        public Builder putExtra(String key, Object value) {
            if (this.extra == null)
                this.extra = new HashMap<>();

            this.extra.put(key, value);
            return this;
        }

        /** Builds the FlagMetadata instance. */
        public FlagMetadata build() {
            return new FlagMetadata(this);
        }
    }
}
