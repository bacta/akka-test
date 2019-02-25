package io.bacta.combat;

import io.bacta.chat.CombatSpam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatureStats {
    private static final Random RNG = new Random(89349);

    private int health;
    private int strength;
    private int constitution;
    private int action;
    private int quickness;
    private int stamina;
    private int mind;
    private int focus;
    private int willpower;

    public boolean isIncapacitated() {
        return this.health <= 0 ||
                this.strength <= 0 ||
                this.constitution <= 0 ||
                this.action <= 0 ||
                this.quickness <= 0 ||
                this.stamina <= 0 ||
                this.mind <= 0 ||
                this.focus <= 0 ||
                this.willpower <= 0;
    }

    public CreatureStats subtract(CreatureStats other) {
        return new CreatureStats(
                health - other.health,
                strength - other.strength,
                constitution - other.constitution,
                action - other.action,
                quickness - other.quickness,
                stamina - other.stamina,
                mind - other.mind,
                focus - other.focus,
                willpower - other.willpower);
    }

    public CreatureStats multiplyScalar(int scalar) {
        return new CreatureStats(
                health * scalar,
                strength * scalar,
                constitution * scalar,
                action * scalar,
                quickness * scalar,
                stamina * scalar,
                mind * scalar,
                focus * scalar,
                willpower * scalar);
    }

    /**
     * Produces a collection of combat spam objects representing any non-zero values for the stats in this
     * CreatureStats object. Typically, you'd call this on the result of some delta between a creature's HAM
     * and some CreatureStats representing the damage to be applied to the creature.
     *
     * @return A collection of CombatSpam messages.
     */
    public List<CombatSpam> createCombatSpam(String attackerName, String defenderName, String attackName) {
        //Not the most efficient since we are creating combat spams that we aren't using.
        //We can clean this by turning the internal fields into an array, and creating manual getters that
        //just refer to indices in that array like `private final int[] stats = new int[9];` and
        //stats[HEALTH] for an accessor of health. Then we could filter the array for 0 first.
        return Stream.of(
                new CombatSpam(attackerName, defenderName, attackName, "health", health),
                new CombatSpam(attackerName, defenderName, attackName, "strength", strength),
                new CombatSpam(attackerName, defenderName, attackName, "constitution", constitution),
                new CombatSpam(attackerName, defenderName, attackName, "action", action),
                new CombatSpam(attackerName, defenderName, attackName, "quickness", quickness),
                new CombatSpam(attackerName, defenderName, attackName, "stamina", stamina),
                new CombatSpam(attackerName, defenderName, attackName, "mind", mind),
                new CombatSpam(attackerName, defenderName, attackName, "focus", focus),
                new CombatSpam(attackerName, defenderName, attackName, "willpower", willpower)
        )
                .filter(spam -> spam.getValue() != 0)
                .collect(Collectors.toList());
    }

    public static CreatureStats random() {
        return new CreatureStats(
                RNG.nextInt(1000) + 500,
                RNG.nextInt(500) + 250,
                RNG.nextInt(500) + 250,
                RNG.nextInt(1000) + 500,
                RNG.nextInt(500) + 250,
                RNG.nextInt(500) + 250,
                RNG.nextInt(1000) + 500,
                RNG.nextInt(500) + 250,
                RNG.nextInt(500) + 250
        );
    }

    @Override
    public String toString() {
        return String.format("{ health: %d, strength: %d, constitution: %d, action: %d, quickness: %d, stamina: %d, mind: %d, focus: %d, willpower: %d }",
                health,
                strength,
                constitution,
                action,
                quickness,
                stamina,
                mind,
                focus,
                willpower);
    }
}
