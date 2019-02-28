package io.bacta.objects;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import io.bacta.name.NameGenerator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

public class ObjectFactoryActor extends AbstractActor {
    private static final int INITIAL_CAPACITY = 10000;
    private static final AtomicLong idGenerator = new AtomicLong();

    public static Props props() {
        return Props.create(ObjectFactoryActor.class, ObjectFactoryActor::new);
    }

    private final TLongObjectMap<ActorRef> objects = new TLongObjectHashMap<>(INITIAL_CAPACITY);

    private void createRandomCreature(CreateRandomCreature message) {
        final long id = idGenerator.getAndIncrement();
        final String name = NameGenerator.generate();
        final ActorRef actorRef = context().actorOf(CreatureObjectActor.props(id, name));

        objects.put(id, actorRef);

        sender().tell(new ObjectCreated(id, actorRef), self());
    }

    @Override

    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateRandomCreature.class, this::createRandomCreature)
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    public static class CreateRandomCreature {
        private final int index;
    }
}
