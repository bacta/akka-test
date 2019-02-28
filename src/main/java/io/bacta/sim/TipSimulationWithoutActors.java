package io.bacta.sim;

import io.bacta.combat.CreatureStats;
import io.bacta.escrow.EscrowTransaction;
import io.bacta.escrow.InsufficientFundsStorageException;
import io.bacta.name.NameGenerator;
import io.bacta.objects.CreatureObject;
import lombok.Getter;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TipSimulationWithoutActors {
    private static final AtomicInteger idGenerator = new AtomicInteger(1);
    private static final Random RNG = new Random();

    private static final int minCashTransfer = 0;
    private static final int maxCashTransfer = 5000;

    private static final int minBankTransfer = 0;
    private static final int maxBankTransfer = 10000;

    private static final int minCashAccount = 0;
    private static final int maxCashAccount = 100000000;

    private static final int minBankAccount = 0;
    private static final int maxBankAccount = 100000000;

    private boolean refundIssued;
    private boolean failed;
    private int cash;
    private int bank;

    public void run() {
        try {
            final CreatureObject source = createParty();
            final CreatureObject target = createParty();

            cash = RNG.nextInt(maxCashTransfer - minCashTransfer + 1) + minCashTransfer;
            bank = RNG.nextInt(maxBankTransfer - minBankTransfer + 1) + minBankTransfer;

            final EscrowTransaction transaction = new EscrowTransaction(source, target, cash, bank);
            transaction.performTransfer();

            this.failed = transaction.isFailed();
            this.refundIssued = transaction.isRefundIssued();
        } catch (InsufficientFundsStorageException ex) {
            //The target failed to get his escrow funds back...what do we do about that?
            //This means that he has made enough credits to put him over the limit.
            //We could store them in some holdings account for the future (with interest?)

            this.failed = true;
        }
    }

    //NOTE 1: We don't need this weird Party state object for the test now because we can query the creature on demand.
    private CreatureObject createParty() {
        final CreatureStats stats = CreatureStats.random();
        final String name = NameGenerator.generate();

        final int cash = RNG.nextInt(maxCashAccount - minCashAccount + 1) + minCashAccount;
        final int bank = RNG.nextInt(maxBankAccount - minBankAccount + 1) + minBankAccount;

        final CreatureObject creature = new CreatureObject(name, stats);
        creature.setCash(cash);
        creature.setBank(bank);

        return creature;
    }
}
