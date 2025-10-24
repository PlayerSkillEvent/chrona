package dev.chrona.economy;

import java.sql.SQLException;
import java.util.UUID;

public interface EconomyService {

    long getBalance(UUID playerId) throws SQLException;

    /** Transfer amount from one player to another, returns new balance of sender
     *
     * @param from Sender player UUID
     * @param to Receiver player UUID
     * @param amount Amount to transfer (must be > 0)
     * @return New balance of sender
     * @throws SQLException on DB error
     */
    long pay(UUID from, UUID to, long amount) throws SQLException;

    /** Claim an amount for a player once, returns claimed amount
     *
     * @param playerId Player UUID
     * @param claimId Unique claim ID for idempotency
     * @param amount Amount to claim (must be > 0)
     * @param source Source description
     * @return Claimed amount
     * @throws SQLException on DB error
     */
    long claimOnce(UUID playerId, UUID claimId, long amount, String source) throws SQLException;

    /** Mint new currency into a player's wallet, returns new balance
     *
     * @param sender Admin/sender UUID
     * @param to Receiver player UUID
     * @param amount Amount to mint (must be > 0)
     * @param corrId Correlation ID for idempotency
     * @return New balance of receiver
     * @throws SQLException on DB error
     */
    long mint(UUID sender, UUID to, long amount, UUID corrId) throws SQLException;

    /** Burn currency from a player's wallet, returns new balance
     *
     * @param sender Admin/sender UUID
     * @param from Player UUID to burn from
     * @param amount Amount to burn (must be > 0)
     * @param corrId Correlation ID for idempotency
     * @return New balance of player
     * @throws SQLException on DB error
     */
    long burn(UUID sender, UUID from, long amount, UUID corrId) throws SQLException;

    Transfer[] getTransfers(UUID playerId, int limit, int offset) throws SQLException;
}
