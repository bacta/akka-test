package io.bacta;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import io.bacta.sim.SimulationSupervisor;
import io.bacta.sim.SimulationSupervisor.StartSimulation;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        ActorSystem actorSystem = null;

        try {
            final int totalCreatures = 1000000;
            final int totalTipSimulations = 1000000;
            final int totalNonActorTipSimulations = 1000000;

            actorSystem = ActorSystem.create("bacta");

            final ActorRef sim = actorSystem.actorOf(SimulationSupervisor.props());
            final StartSimulation startSimulation = new StartSimulation(
                    totalCreatures,
                    totalTipSimulations,
                    totalNonActorTipSimulations);

            sim.tell(startSimulation, ActorRef.noSender());

            System.in.read();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (actorSystem != null) {
                actorSystem.terminate();
            }
        }
    }
}
