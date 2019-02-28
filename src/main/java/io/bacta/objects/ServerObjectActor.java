package io.bacta.objects;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class ServerObjectActor extends AbstractActor {
    private final long id;

    @Setter
    private String name;

    protected ServerObjectActor(long id) {
        this.id = id;
        this.name = String.format("Object %d", id);
    }

    @Override
    public final Receive createReceive() {
        return this.appendReceiveHandlers(receiveBuilder()).build();
    }

    protected ReceiveBuilder appendReceiveHandlers(ReceiveBuilder receiveBuilder) {
        return receiveBuilder
                .matchAny(this::unhandledMessage);
    }

    private void unhandledMessage(Object msg) {
        System.out.println("unhandled message");
    }
}
