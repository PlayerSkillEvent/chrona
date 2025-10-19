package dev.chrona.economy.application;

import dev.chrona.common.economy.TransactionOrigin;

import java.util.UUID;

public interface WalletService {

    /**
     * Credits the specified amount to the player's wallet,
     * if the claim (claimId) does not already exist.
     * Performs an idempotent credit and logs the transaction.
     * @param playerId the unique player identifier
     * @param amount the amount to credit
     * @param claimId the unique claim identifier
     * @param origin the origin of the credit
     * @throws RuntimeException if a database error occurs.
     */
    void credit(UUID playerId, long amount, UUID claimId, TransactionOrigin origin);

    /**
     * Debits the specified amount from the player's wallet,
     * if the claim (claimId) does not already exist.
     * Performs an idempotent debit and logs the transaction.
     * @param playerId the unique player identifier
     * @param amount the amount to debit
     * @param claimId the unique claim identifier
     * @param origin the source of the debit
     * @throws RuntimeException if a database error occurs.
     */
    void debit (UUID playerId, long amount, UUID claimId, TransactionOrigin origin);
}
