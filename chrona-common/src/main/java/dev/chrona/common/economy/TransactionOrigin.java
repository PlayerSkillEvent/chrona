package dev.chrona.common.economy;

import java.util.Objects;

public record TransactionOrigin(SourceDomain domain, String key) {

    public TransactionOrigin {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(key, "key");
        if (key.isBlank())
            throw new IllegalArgumentException("key blank");
    }

    public static TransactionOrigin of(SourceDomain domain, String key) {
        return new TransactionOrigin(domain, key);
    }

    public static TransactionOrigin of(ChronaSource src) {
        return new TransactionOrigin(src.domain(), src.key());
    }

    @Override
    public String toString() {
        return domain + ":" + key;
    }
}
