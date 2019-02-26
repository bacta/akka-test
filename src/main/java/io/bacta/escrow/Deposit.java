package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Deposit {
    private final int cash;
    private final int bank;

    public Withdrawal toWithdrawal() {
        return new Withdrawal(cash, bank);
    }
}
