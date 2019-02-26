package io.bacta;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import io.bacta.sim.BattleSimulationActor;
import io.bacta.sim.BattleSimulationResult;
import io.bacta.sim.TipSimulationActor;
import io.bacta.sim.TipSimulationResult;
import scala.concurrent.ExecutionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App extends AbstractActor {
    private final static Random RNG = new Random();
    private final static int minBattleParticipants = 5;
    private final static int maxBattleParticipants = 10;

    public static Props props(int totalBattleSimulations, int totalTipSimulations) {
        return Props.create(App.class, () -> new App(totalBattleSimulations, totalTipSimulations));
    }

    private final List<ActorRef> battleSimulations;
    private final List<ActorRef> tipSimulations;

    private final int totalBattleSimulations;
    private final int totalTipSimulations;

    private long startTimestamp;

    private final List<BattleSimulationResult> battleSimulationResults;
    private final List<TipSimulationResult> tipSimulationResults;

    public App(int totalBattleSimulations, int totalTipSimulations) {
        this.totalBattleSimulations = totalBattleSimulations;
        this.totalTipSimulations = totalTipSimulations;

        this.battleSimulationResults = new ArrayList<>(totalBattleSimulations);
        this.tipSimulationResults = new ArrayList<>(totalTipSimulations);

        this.battleSimulations = Stream.generate(this::createBattleSimulation).limit(totalBattleSimulations).collect(Collectors.toList());
        this.tipSimulations = Stream.generate(this::createTipSimulation).limit(totalTipSimulations).collect(Collectors.toList());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::start)
                .match(BattleSimulationResult.class, this::receiveBattleSimulationResult)
                .match(BattleSimulationsComplete.class, this::battleSimulationsCompleted)
                .match(TipSimulationResult.class, this::receiveTipSimulationResult)
                .match(TipSimulationsComplete.class, this::tipSimulationsCompleted)
                .build();
    }

    private void start(StartSimulation msg) {
        System.out.printf("Simulating %d battles and %d tips...\n", totalBattleSimulations, totalTipSimulations);

        final Materializer materializer = ActorMaterializer.create(context().system());
        final ExecutionContext ec = context().system().dispatcher();

        startTimestamp = System.currentTimeMillis();

        Source.from(battleSimulations)
                .to(Sink.foreachParallel(4, sim -> sim.tell(new BattleSimulationActor.StartBattle(), self()), ec))
                .run(materializer);

        final int minTipsPerMs = 1000;
        final int maxTipsPerMs = 5000;

        Source.from(tipSimulations)
                //.throttle(minTipsPerMs,FiniteDuration.apply(50, TimeUnit.MILLISECONDS), maxTipsPerMs, ThrottleMode.shaping())
                .to(Sink.foreachParallel(4, sim -> sim.tell(new TipSimulationActor.StartSimulation(), self()), ec))
                .run(materializer);
    }

    private ActorRef createBattleSimulation() {
        final int totalParticipants = RNG.nextInt(maxBattleParticipants - minBattleParticipants) + minBattleParticipants;

        return context().actorOf(BattleSimulationActor.props(totalParticipants));
    }

    private ActorRef createTipSimulation() {
        return context().actorOf(TipSimulationActor.props());
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
        final long finishTimestmap = System.currentTimeMillis();
        final long deltaTimestamp = finishTimestmap - startTimestamp;

        System.out.printf("%d transfers processed in %d milliseconds.\n",
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

    public static void main(String[] args) {
        final int totalBattleSimulations = 1;
        final int totalTipSimulations = 500000;

        final ActorSystem actorSystem = ActorSystem.create("bacta");
        final ActorRef sim = actorSystem.actorOf(App.props(totalBattleSimulations, totalTipSimulations));

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
