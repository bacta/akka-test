package io.bacta.sim;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TipSimulationResult {
    private final int cash;
    private final int bank;
    private final boolean failed;
    private final boolean refunded;
}
