package io.bacta.combat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AttackApplied {
    private final CreatureStats ham;
    private final CreatureStats appliedDamage;
}
