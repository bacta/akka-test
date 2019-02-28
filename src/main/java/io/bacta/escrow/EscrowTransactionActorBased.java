package io.bacta.escrow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EscrowTransactionActorBased {
    private final long id;
    private final Party source;
    private final Party target;
    private final int cash;
    private final int bank;

    public DepositReq toDepositWithAcknowledgement() {
        return new DepositReq(id, cash, bank, true);
    }

    public DepositReq toDepositWithoutAcknowledgement() {
        return new DepositReq(id, cash, bank, false);
    }

    public WithdrawalReq toWithdrawalWithAcknowledgement() {
        return new WithdrawalReq(id, cash, bank, true);
    }

    public WithdrawalReq toWithdrawalWithoutAcknowledgement() {
        return new WithdrawalReq(id, cash, bank, false);
    }
}
