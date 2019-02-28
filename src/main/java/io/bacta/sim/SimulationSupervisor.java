package io.bacta.sim;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import io.bacta.escrow.InterplanetaryEscrowServiceActor;
import io.bacta.escrow.TransferComplete;
import io.bacta.escrow.TransferFailed;
import io.bacta.objects.ObjectCreated;
import io.bacta.objects.ObjectFactoryActor;
import io.bacta.objects.ObjectFactoryActor.CreateRandomCreature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scala.concurrent.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimulationSupervisor extends AbstractActor {
    private final static Random RNG = new Random();
    private final static int MIN_CASH_TRANSFER = 1000;
    private final static int MAX_CASH_TRANSFER = 1000000;
    private final static int MIN_BANK_TRANSFER = 100000;
    private final static int MAX_BANK_TRANSFER = 100000000;

    public static Props props() {
        return Props.create(SimulationSupervisor.class, SimulationSupervisor::new);
    }

    private final ActorRef escrowService;
    private final ActorRef objectService;

    private List<ActorRef> creatures;
    private long startTimestamp;

    private int totalCreatures;
    private int totalTipSimulations;
    private int totalTipSimulationsWithoutActors;

    private List<TipSimulationResult> tipSimulationResults;

    private final Materializer materializer = ActorMaterializer.create(context().system());
    private final ExecutionContext ec = context().system().dispatcher();

    public SimulationSupervisor() {
        this.escrowService = context().actorOf(InterplanetaryEscrowServiceActor.props());
        this.objectService = context().actorOf(ObjectFactoryActor.props());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::start)
                .match(ObjectCreated.class, this::objectCreated)
                .match(CreaturesCreated.class, this::creaturesCreated)
                .match(StartTipSimulations.class, this::startTipSimulations)
                .match(StartTipSimulationsWithoutActors.class, this::startTipSimulationsWithoutActors)
                .match(TipSimulationsCompleted.class, this::finishTipSimulations)
                .match(TransferComplete.class, this::tipTransactionComplete)
                .match(TransferFailed.class, this::tipTransactionFailed)
                .build();
    }

    private void start(StartSimulation msg) {
        this.totalCreatures = msg.totalCreatures;
        this.totalTipSimulations = msg.totalTipSimulations;
        this.totalTipSimulationsWithoutActors = msg.totalTipSimulationsWithoutActors;

        this.creatures = new ArrayList<>(msg.totalCreatures);
        this.tipSimulationResults = new ArrayList<>(msg.totalTipSimulations);

        System.out.println("Starting simulation...");
        System.out.printf("Creating %d creatures...", this.totalCreatures);

        startTimestamp = System.currentTimeMillis();

        //First, create all the creatures we need.
        Source.range(0, msg.totalCreatures)
                .map(CreateRandomCreature::new)
                .to(Sink.foreachParallel(4, this::requestRandomCreature, ec))
                .run(materializer);
    }

    private void objectCreated(ObjectCreated msg) {
        creatures.add(msg.getObject());

        if (creatures.size() == totalCreatures) {
            self().tell(new CreaturesCreated(), self());
        }
    }

    private void creaturesCreated(CreaturesCreated msg) {
        final long finishTimestamp = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestamp - startTimestamp;

        System.out.printf("done in %d ms.\n", deltaTimestamp);

        self().tell(new StartTipSimulations(), self());
    }

    private void startTipSimulations(StartTipSimulations msg) {
        System.out.printf("Running %d tip simulations with actors...", totalTipSimulations);

        startTimestamp = System.currentTimeMillis();

        Source.range(0, totalTipSimulations)
                .map(this::createStartTipSimulationMessage)
                .to(Sink.foreachParallel(4, this::requestTipTransaction, ec))
                .run(materializer);
    }

    private void startTipSimulationsWithoutActors(StartTipSimulationsWithoutActors msg) {
        System.out.printf("Running %d tip simulations without actors...", totalTipSimulationsWithoutActors);

        startTimestamp = System.currentTimeMillis();

        final List<TipSimulationResult> results = Stream.generate(TipSimulationWithoutActors::new)
                .limit(totalTipSimulationsWithoutActors)
                .parallel()
                .map(this::executeTipSimulationWithoutActors)
                .collect(Collectors.toList());

        final long finishTimestamp = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestamp - startTimestamp;

        printTipSimulationResults(deltaTimestamp, results);
    }

    private void finishTipSimulations(TipSimulationsCompleted msg) {
        final long finishTimestamp = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestamp - startTimestamp;

        printTipSimulationResults(deltaTimestamp, tipSimulationResults);

        self().tell(new StartTipSimulationsWithoutActors(), self());
    }

    private void requestRandomCreature(CreateRandomCreature msg) {
        objectService.tell(msg, self());
    }

    private void requestTipTransaction(InterplanetaryEscrowServiceActor.StartTransaction msg) {
        escrowService.tell(msg, self());
    }

    private TipSimulationResult executeTipSimulationWithoutActors(TipSimulationWithoutActors sim) {
        sim.run();
        return new TipSimulationResult(sim.getCash(), sim.getBank(), sim.isFailed(), sim.isRefundIssued());
    }

    private void tipTransactionComplete(TransferComplete msg) {
        tipSimulationResults.add(new TipSimulationResult(msg.getCash(), msg.getBank(), false, false));

        if (tipSimulationResults.size() == totalTipSimulations) {
            self().tell(new TipSimulationsCompleted(), self());
        }
    }

    private void tipTransactionFailed(TransferFailed msg) {
        tipSimulationResults.add(new TipSimulationResult(0, 0, true, msg.isRefunded()));

        if (tipSimulationResults.size() == totalTipSimulations) {
            self().tell(new TipSimulationsCompleted(), self());
        }
    }

    private InterplanetaryEscrowServiceActor.StartTransaction createStartTipSimulationMessage(int index) {
        final ActorRef source = getRandomObject(null);
        final ActorRef target = getRandomObject(source);
        final int cash = RNG.nextInt(MAX_CASH_TRANSFER - MIN_CASH_TRANSFER + 1) + MIN_CASH_TRANSFER;
        final int bank = RNG.nextInt(MAX_BANK_TRANSFER - MIN_BANK_TRANSFER + 1) + MAX_BANK_TRANSFER;

        return new InterplanetaryEscrowServiceActor.StartTransaction(source, target, cash, bank);
    }

    private ActorRef getRandomObject(ActorRef exclude) {
        final int index = RNG.nextInt(totalCreatures);
        final ActorRef actorRef = creatures.get(index);

        return actorRef == exclude
                ? getRandomObject(exclude)
                : actorRef;
    }

    private void printTipSimulationResults(long deltaTimestamp, List<TipSimulationResult> tipSimulationResults) {
        System.out.printf("%d processed in %d milliseconds.\n",
                tipSimulationResults.size(),
                deltaTimestamp);

        final long totalCash = tipSimulationResults.stream().mapToLong(TipSimulationResult::getCash).sum();
        final long totalBank = tipSimulationResults.stream().mapToLong(TipSimulationResult::getBank).sum();
        final int totalRefunds = tipSimulationResults.stream().mapToInt(r -> r.isRefunded() ? 1 : 0).sum();
        final int totalFailures = tipSimulationResults.stream().mapToInt(r -> r.isFailed() ? 1 : 0).sum();

        System.out.printf("- Cash Credits Transferred: %d\n", totalCash);
        System.out.printf("- Bank Credits Transferred: %d\n", totalBank);
        System.out.printf("- Failures: %d\n", totalRefunds);
        System.out.printf("- Refunds: %d\n", totalFailures);
    }

    @Getter
    @RequiredArgsConstructor
    public static class StartSimulation {
        private final int totalCreatures;
        private final int totalTipSimulations;
        private final int totalTipSimulationsWithoutActors;
    }

    private static class CreaturesCreated {
    }

    private static class StartTipSimulations {
    }

    private static class StartTipSimulationsWithoutActors {
    }

    private static class TipSimulationsCompleted {
    }
}
