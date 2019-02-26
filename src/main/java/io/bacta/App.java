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
    private final static int minBattleParticipants = 10;
    private final static int maxBattleParticipants = 30;


    public static Props props(int totalBattleSimulations, int totalTipSimulations) {
        return Props.create(App.class, () -> new App(totalBattleSimulations, totalTipSimulations));
    }

    private final List<ActorRef> battleSimulations;
    private final List<ActorRef> tipSimulations;

    private final int totalBattleSimulations;
    private final int totalTipSimulations;

    private long startTimestamp;

    private final List<BattleSimulationResult> battleSimulationResults;

    public App(int totalBattleSimulations, int totalTipSimulations) {
        this.totalBattleSimulations = totalBattleSimulations;
        this.totalTipSimulations = totalTipSimulations;

        this.battleSimulationResults = new ArrayList<>(totalBattleSimulations);

        this.battleSimulations = Stream.generate(this::createBattleSimulation).limit(totalBattleSimulations).collect(Collectors.toList());
        this.tipSimulations = Stream.generate(this::createTipSimulation).limit(totalTipSimulations).collect(Collectors.toList());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::start)
                .match(BattleSimulationResult.class, this::receiveBattleSimulationResult)
                .match(BattleSimulationsComplete.class, this::battleSimulationsCompleted)
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

    public static void main(String[] args) {
        final int totalBattleSimulations = 1000;
        final int totalTipSimulations = 10;

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
