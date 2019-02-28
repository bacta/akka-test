package io.bacta.sim;

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

    private final static int MIN_CASH_TRANSFER = 1000;
    private final static int MAX_CASH_TRANSFER = 1000000;
    private final static int MIN_BANK_TRANSFER = 100000;
    private final static int MAX_BANK_TRANSFER = 100000000;

    private boolean refundIssued;
    private boolean failed;
    private int cash;
    private int bank;

    public void run() {
        try {
            final CreatureObject source = createParty();
            final CreatureObject target = createParty();

            cash = RNG.nextInt(MAX_CASH_TRANSFER - MIN_CASH_TRANSFER + 1) + MIN_CASH_TRANSFER;
            bank = RNG.nextInt(MAX_BANK_TRANSFER - MIN_BANK_TRANSFER + 1) + MIN_BANK_TRANSFER;

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
        final String name = NameGenerator.generate();
        return new CreatureObject(name);
    }
}
