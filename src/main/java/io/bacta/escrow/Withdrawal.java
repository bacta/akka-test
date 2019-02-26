package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Withdrawal {
    private final int cash;
    private final int bank;

    public Deposit toDeposit() {
        return new Deposit(cash, bank);
    }
}
