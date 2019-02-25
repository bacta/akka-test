package io.bacta.combat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Random;

public class AttackTypes {
    private static final Random RNG = new Random(12949083);

    public static final Attack HEADSHOT = new Attack(
            "headshot1",
            new CreatureStats(0, 0, 0, 100, 0, 0, 0, 0, 0),
            new CreatureStats(20, 0, 0, 0, 0, 0, 0, 0, 0)
    );

    public static final Attack LEGSHOT = new Attack(
            "legshot1",
            new CreatureStats(0, 0, 0, 150, 0, 0, 0, 0, 0),
            new CreatureStats(10, 0, 0, 0, 0, 0, 0, 0, 0)
    );

    public static final Attack MINDSHOT = new Attack(
            "mindshot1",
            new CreatureStats(0, 0, 0, 250, 0, 0, 0, 0, 0),
            new CreatureStats(30, 0, 0, 0, 0, 0, 0, 0, 0)
    );

    public static final Attack FORCECHOKE = new Attack(
            "forcechoke1",
            new CreatureStats(20, 0, 0, 300, 0, 0, 0, 0, 0),
            new CreatureStats(200, 0, 0, 0, 0, 0, 0, 0, 0)
    );

    private static final Attack[] attacks = new Attack[] {
            HEADSHOT,
            LEGSHOT,
            MINDSHOT,
            FORCECHOKE
    };

    public static Attack chooseRandomAttack() {
        final int index = RNG.nextInt(attacks.length - 1);
        return attacks[index];
    }


    @Getter
    @RequiredArgsConstructor
    public static class Attack {
        private final String name;
        private final CreatureStats cost;
        private final CreatureStats damage;
    }
}
