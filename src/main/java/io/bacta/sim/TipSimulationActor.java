package io.bacta.sim;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.concurrent.atomic.AtomicInteger;

public class TipSimulationActor extends AbstractActor {
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    public static Props props() {
        return Props.create(TipSimulationActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }
}
