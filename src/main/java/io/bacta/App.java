package io.bacta;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import io.bacta.sim.*;
import scala.concurrent.ExecutionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App extends AbstractActor {
    private final static Random RNG = new Random();
    private final static int minBattleParticipants = 5;
    private final static int maxBattleParticipants = 10;

    public static Props props(int totalBattleSimulations, int totalTipSimulations, int totalNonActorTipSimulations) {
        return Props.create(App.class, () -> new App(totalBattleSimulations, totalTipSimulations, totalNonActorTipSimulations));
    }

    private final List<ActorRef> battleSimulations;
    private final List<ActorRef> tipSimulations;
    private final List<TipSimulationWithoutActors> nonActorTipSimulations;

    private final int totalBattleSimulations;
    private final int totalTipSimulations;
    private final int totalNonActorTipSimulations;

    private long startTimestamp;

    private final List<BattleSimulationResult> battleSimulationResults;
    private final List<TipSimulationResult> tipSimulationResults;
    private final List<TipSimulationResult> nonActorTipSimulationsResults;
    private final AtomicInteger nonActorTipResultCounter = new AtomicInteger(0);

    public App(int totalBattleSimulations, int totalTipSimulations, int totalNonActorTipSimulations) {
        this.totalBattleSimulations = totalBattleSimulations;
        this.totalTipSimulations = totalTipSimulations;
        this.totalNonActorTipSimulations = totalNonActorTipSimulations;

        this.battleSimulationResults = new ArrayList<>(totalBattleSimulations);
        this.tipSimulationResults = new ArrayList<>(totalTipSimulations);
        this.nonActorTipSimulationsResults = new ArrayList<>(totalNonActorTipSimulations);

        this.battleSimulations = Stream.generate(this::createBattleSimulation).limit(totalBattleSimulations).collect(Collectors.toList());
        this.tipSimulations = Stream.generate(this::createTipSimulation).limit(totalTipSimulations).collect(Collectors.toList());
        this.nonActorTipSimulations = Stream.generate(this::createNonActorTipSimulation).limit(totalNonActorTipSimulations).collect(Collectors.toList());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::start)
                .match(BattleSimulationResult.class, this::receiveBattleSimulationResult)
                .match(BattleSimulationsComplete.class, this::battleSimulationsCompleted)
                .match(TipSimulationResult.class, this::receiveTipSimulationResult)
                .match(TipSimulationsComplete.class, this::tipSimulationsCompleted)
                .match(NonActorTipSimulationsComplete.class, this::nonActorTipSimulationsCompleted)
                .build();
    }

    private void start(StartSimulation msg) {
        System.out.printf("Simulating %d battles and %d tips (actor: %d, non-actor: %d)...\n",
                totalBattleSimulations,
                totalTipSimulations + totalNonActorTipSimulations,
                totalTipSimulations,
                totalNonActorTipSimulations);

        final Materializer materializer = ActorMaterializer.create(context().system());
        final ExecutionContext ec = context().system().dispatcher();

        startTimestamp = System.currentTimeMillis();

        Source.from(battleSimulations)
                .to(Sink.foreachParallel(4, sim -> sim.tell(new BattleSimulationActor.StartBattle(), self()), ec))
                .run(materializer);

        Source.from(tipSimulations)
                .to(Sink.foreachParallel(4, sim -> sim.tell(new TipSimulationActor.StartSimulation(), self()), ec))
                .run(materializer);

        Source.from(nonActorTipSimulations)
                .to(Sink.foreachParallel(4, sim -> {
                    sim.run();

                    this.nonActorTipSimulationsResults.add(new TipSimulationResult(sim.getCash(), sim.getBank(), sim.isFailed(), sim.isRefundIssued()));

                    final int total = nonActorTipResultCounter.incrementAndGet();

//                    if (total % 10000 == 0) {
//                        System.out.printf("Completed non actor tip. %d of %d\n", total, totalNonActorTipSimulations);
//                    }

                    if (total >= totalNonActorTipSimulations) {
                        self().tell(new NonActorTipSimulationsComplete(), self());
                    }
                }, ec))
                .run(materializer);
    }

    private ActorRef createBattleSimulation() {
        final int totalParticipants = RNG.nextInt(maxBattleParticipants - minBattleParticipants) + minBattleParticipants;

        return context().actorOf(BattleSimulationActor.props(totalParticipants));
    }

    private ActorRef createTipSimulation() {
        return context().actorOf(TipSimulationActor.props());
    }

    private TipSimulationWithoutActors createNonActorTipSimulation() {
        return new TipSimulationWithoutActors();
    }

    private void receiveBattleSimulationResult(BattleSimulationResult result) {
        battleSimulationResults.add(result);

        //Stop the simulation actor.
        context().stop(sender());

        if (battleSimulationResults.size() == this.totalBattleSimulations) {
            self().tell(new BattleSimulationsComplete(), self());
        }
    }

    private void receiveTipSimulationResult(TipSimulationResult result) {
        tipSimulationResults.add(result);

        context().stop(sender());

        if (tipSimulationResults.size() == this.totalTipSimulations) {
            self().tell(new TipSimulationsComplete(), self());
        }
    }

    private void battleSimulationsCompleted(BattleSimulationsComplete msg) {
        final long finishTimestamp = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestamp - startTimestamp;

        System.out.printf("Collected %d battle simulation results in %d milliseconds.\n",
                battleSimulationResults.size(),
                deltaTimestamp);

        battleSimulationResults.stream()
                .sorted(Comparator.comparingInt(BattleSimulationResult::getId))
                .forEachOrdered(this::printBattleSimulationResult);
    }

    private void tipSimulationsCompleted(TipSimulationsComplete msg) {
        final long finishTimestamp = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestamp - startTimestamp;

        System.out.printf("%d actor based transfers processed in %d milliseconds.\n",
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

    private void nonActorTipSimulationsCompleted(NonActorTipSimulationsComplete msg) {
        final long finishTimestamp = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestamp - startTimestamp;

        System.out.printf("%d non-actor based transfers processed in %d milliseconds.\n",
                nonActorTipSimulationsResults.size(),
                deltaTimestamp);

        final long totalCash = nonActorTipSimulationsResults.stream().mapToLong(TipSimulationResult::getCash).sum();
        final long totalBank = nonActorTipSimulationsResults.stream().mapToLong(TipSimulationResult::getBank).sum();
        final int totalRefunds = nonActorTipSimulationsResults.stream().mapToInt(r -> r.isRefunded() ? 1 : 0).sum();
        final int totalFailures = nonActorTipSimulationsResults.stream().mapToInt(r -> r.isFailed() ? 1 : 0).sum();

        System.out.printf("- Cash Credits Transferred: %d\n", totalCash);
        System.out.printf("- Bank Credits Transferred: %d\n", totalBank);
        System.out.printf("- Failures: %d\n", totalRefunds);
        System.out.printf("- Refunds: %d\n", totalFailures);
    }

    private void printBattleSimulationResult(BattleSimulationResult result) {
        System.out.printf("%-6d: After %d rounds and %d total participants, the winners were: ",
                result.getId(),
                result.getRounds(),
                result.getTotalParticipants());

        final String winners;
        if (result.getWinningParticipants().size() <= 0) {
            winners = "There were no winners. Everyone was incapacitated.";
        } else {
            winners = String.join(", ", result.getWinningParticipants());
        }

        System.out.println(winners);
    }

    private static class StartSimulation {
    }

    private static class BattleSimulationsComplete {
    }

    private static class TipSimulationsComplete {
    }

    private static class NonActorTipSimulationsComplete {
    }

    public static void main(String[] args) {
        final int totalBattleSimulations = 0;
        final int totalTipSimulations = 0;
        final int totalNonActorTipSimulations = 2000000;

        final ActorSystem actorSystem = ActorSystem.create("bacta");
        final ActorRef sim = actorSystem.actorOf(App.props(totalBattleSimulations, totalTipSimulations, totalNonActorTipSimulations));

        sim.tell(new StartSimulation(), ActorRef.noSender());

        try {
            System.in.read();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            actorSystem.terminate();
        }
    }
}
