package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DepositAck {
    private final long transactionId;
    private final String name;
    private final int cash;
    private final int bank;
}
