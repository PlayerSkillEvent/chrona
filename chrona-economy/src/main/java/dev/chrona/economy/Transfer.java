package dev.chrona.economy;

import java.util.UUID;

public record Transfer(UUID uuid, UUID from, UUID to, long amount, TransferReason reason, UUID corrId, long timestamp) {

    /** Creates a new Transfer with a random UUID.
     *
     * @param from   Sender UUID
     * @param to     Reciever UUID
     * @param amount Amount to transfer
     * @param reason Reason for the transfer
     * @param corrId Correlation ID for idempotency
     */
    public Transfer(UUID from, UUID to, long amount, TransferReason reason, UUID corrId) {
        this(null, from, to, amount, reason, corrId, 0);
    }

    public Transfer {
        if (uuid == null)
            uuid = UUID.randomUUID();

        if (timestamp == 0)
            timestamp = System.currentTimeMillis();
    }

    public enum TransferReason  {
        CLAIM("CLAIM"),
        PLAYER_PAYMENT("PAY"),
        QUEST_REWARD("QUEST"),
        EVENT_REWARD("EVENT"),
        JOB_REWARD("JOB"),
        ADMIN_MINT("ADMIN_MINT"),
        ADMIN_BURN("ADMIN_BURN");

        private final String name;

        TransferReason(String name) {
            this.name = name;
        }

        public String value() {
            return name;
        }
    }
}
