package io.bacta.escrow;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Party {
    private final ActorRef actor;
    private String name;
}