package io.bacta.objects;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public abstract class ServerObjectActor extends AbstractActor {
    private static final AtomicLong idGenerator = new AtomicLong(1);

    private final long id;

    @Setter
    private String name;

    public ServerObjectActor() {
        this.id = idGenerator.getAndIncrement();
        this.name = String.format("Object %d", id);
    }

    @Override
    public final Receive createReceive() {
        return this.appendReceiveHandlers(receiveBuilder()).build();
    }

    protected ReceiveBuilder appendReceiveHandlers(ReceiveBuilder receiveBuilder){
        return receiveBuilder
                .matchAny(this::unhandledMessage);
    }

    private void unhandledMessage(Object msg) {
        System.out.println("unhandled message");
    }
}
