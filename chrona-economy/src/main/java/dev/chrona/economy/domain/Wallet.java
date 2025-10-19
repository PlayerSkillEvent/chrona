package dev.chrona.economy.domain;

import java.util.UUID;

public final class Wallet {
    private final UUID playerId;
    private long balance;
    private int version;

    public Wallet(UUID id,long bal,int ver) {
        this.playerId = id;
        this.balance = bal;
        this.version=ver;
    }

    public UUID playerId() {
        return playerId;
    }

    public long balance() {
        return balance;
    }

    public int version() {
        return version;
    }

    public void credit(long d) {
        balance = Math.addExact(balance, d);
    }

    public void debit(long d) {
        if(balance < d)
            throw new IllegalStateException("insufficient");
        balance -= d;
    }

    public void bump() {
        version++;
    }
}
