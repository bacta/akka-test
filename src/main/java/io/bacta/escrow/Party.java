package io.bacta.escrow;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Party {
    private final ActorRef actor;
    private final String name;
}