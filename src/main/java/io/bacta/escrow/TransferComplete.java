package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TransferComplete {
    private final int cash;
    private final int bank;
}
