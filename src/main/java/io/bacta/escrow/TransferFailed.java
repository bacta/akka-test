package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TransferFailed {
    private final long transactionId;
    private final String reason;
    private final boolean refunded;
}
