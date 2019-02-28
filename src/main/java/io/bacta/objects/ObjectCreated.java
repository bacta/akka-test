package io.bacta.objects;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ObjectCreated {
    private final long id;
    private final ActorRef object;
}
