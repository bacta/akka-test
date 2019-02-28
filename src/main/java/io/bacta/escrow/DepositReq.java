package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DepositReq {
    private final long transactionId;
    private final int cash;
    private final int bank;
    private final boolean ack;

    public DepositAck toAck(String name) {
        return new DepositAck(transactionId, name, cash, bank);
    }
}
