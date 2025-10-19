package dev.chrona.economy.domain;

import java.util.*;

public interface WalletRepository {

    /* Returns the wallet for the given player ID, if it exists. */
    Optional<Wallet> find(UUID playerId);

    /* Inserts a new wallet into the database. */
    void insert(Wallet w);

    /*
     * Updates the wallet using optimistic locking.
     * @return true if the update was successful, false if the version check failed.
    */
    boolean update(Wallet w);
}
