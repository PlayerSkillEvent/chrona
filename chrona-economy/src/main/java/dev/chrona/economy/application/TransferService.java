package dev.chrona.economy.application;

import dev.chrona.common.economy.TransactionOrigin;

import java.util.UUID;

public interface TransferService {
    /**
     * Überweist atomar amount von 'from' nach 'to'.
     * Idempotent über transferId: gleicher transferId-Aufruf => NOOP.
     */
    void transfer(UUID from, UUID to, long amount, UUID transferId, TransactionOrigin origin);
}
