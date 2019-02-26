package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TransferFailed {
    private final boolean refunded;
}
