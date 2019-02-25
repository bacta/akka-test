package io.bacta.combat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AttackStats {
    private final String name;
    private final CreatureStats ham;
    private final int weaponDamage;
}
