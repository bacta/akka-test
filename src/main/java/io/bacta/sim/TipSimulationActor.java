package io.bacta.sim;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.bacta.combat.CreatureStats;
import io.bacta.escrow.EscrowBroker;
import io.bacta.escrow.Party;
import io.bacta.escrow.TransferComplete;
import io.bacta.escrow.TransferFailed;
import io.bacta.messages.creature.SetPropertiesMessage;
import io.bacta.name.NameGenerator;
import io.bacta.objects.CreatureObjectActor;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TipSimulationActor extends AbstractActor {
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

    public static Props props() {
        return Props.create(TipSimulationActor.class, TipSimulationActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::start)
                .match(TransferFailed.class, this::transferFailed)
                .match(TransferComplete.class, this::transferComplete)
                .build();
    }

    private void start(StartSimulation msg) {
        //The involved parties.
        final Party source = createParty();
        final Party target = createParty();

        //The amounts to transfer.
        final int cashTransfer = RNG.nextInt(maxCashTransfer - minCashTransfer + 1) + minCashTransfer;
        final int bankTransfer = RNG.nextInt(maxBankTransfer - minBankTransfer + 1) + minBankTransfer;

//        System.out.printf("Starting transfer between %s and %s (cash: %d, bank: %d)\n",
//                source.getName(),
//                target.getName(),
//                cashTransfer,
//                bankTransfer);

        context().actorOf(EscrowBroker.props(source, target, cashTransfer, bankTransfer));
    }

    private void transferComplete(TransferComplete msg) {
        context().parent().tell(new TipSimulationResult(msg.getCash(), msg.getBank(), false, false), self());
    }

    private void transferFailed(TransferFailed msg) {
        context().parent().tell(new TipSimulationResult(0, 0, true, msg.isRefunded()), self());
    }

    private Party createParty() {
        final CreatureStats stats = CreatureStats.random();
        final String name = NameGenerator.generate();

        final int cash = RNG.nextInt(maxCashAccount - minCashAccount + 1) + minCashAccount;
        final int bank = RNG.nextInt(maxBankAccount - minBankAccount + 1) + minBankAccount;

        final ActorRef creature = context().actorOf(CreatureObjectActor.props(stats, name));
        final SetPropertiesMessage properties = new SetPropertiesMessage();
        properties.put("cash", cash);
        properties.put("bank", bank);

        creature.tell(properties, self());

        return new Party(creature, name);
    }

    public static class StartSimulation {
    }
}
