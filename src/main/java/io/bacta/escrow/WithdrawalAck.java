package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WithdrawalAck {
    private final long transactionId;
    private final String name; //We also send the name of the creature back in the withdrawal message.
    private final int cash;
    private final int bank;
}
