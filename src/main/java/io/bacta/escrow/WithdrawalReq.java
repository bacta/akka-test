package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WithdrawalReq {
    private final long transactionId;
    private final int cash;
    private final int bank;
    private final boolean ack;

    public WithdrawalAck toAck(String name) {
        return new WithdrawalAck(transactionId, name, cash, bank);
    }
}
