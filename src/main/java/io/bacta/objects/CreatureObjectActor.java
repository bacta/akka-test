package io.bacta.objects;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import io.bacta.chat.CombatSpam;
import io.bacta.chat.SystemMessage;
import io.bacta.combat.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
@Setter
public class CreatureObjectActor extends TangibleObjectActor {
    private static final Random RNG = new Random();
    private static final int minWeaponDamage = 2;
    private static final int maxWeaponDamage = 36;

    public static Props props(CreatureStats stats, String name) {
        return Props.create(CreatureObjectActor.class, () -> {
            final CreatureObjectActor actor = new CreatureObjectActor(stats);
            actor.setName(name);

            return actor;
        });
    }

    //Just some defaults for now. Eventually, we will generate weapon and armor objects too.
    private CreatureStats ham = new CreatureStats();
    private AttackStats attackStats = new AttackStats(getName(), this.ham, RNG.nextInt(maxWeaponDamage - minWeaponDamage) + minWeaponDamage);
    private DefenseStats defenseStats = new DefenseStats(getName(), this.ham);

    public CreatureObjectActor(CreatureStats initialStats) {
        this.setHam(initialStats);
    }

    @Override
    protected ReceiveBuilder appendReceiveHandlers(ReceiveBuilder receiveBuilder) {
        return super.appendReceiveHandlers(
                receiveBuilder
                        .match(AttackStatsRequest.class, this::sendAttackStats)
                        .match(DefenseStatsRequest.class, this::sendDefenseStats)
                        .match(ApplyAttack.class, this::applyAttack)
                        .match(AttackFailed.class, this::attackFailed)
                        .match(SystemMessage.class, this::systemMessage)
                        .match(CombatSpam.class, this::combatSpam)
        );
    }

    @Override
    public void setName(String name) {
        super.setName(name);

        this.attackStats = new AttackStats(getName(), ham, attackStats.getWeaponDamage());
        this.defenseStats = new DefenseStats(getName(), ham);
    }

    public void setHam(CreatureStats stats) {
        this.ham = stats;

        this.attackStats = new AttackStats(getName(), stats, attackStats.getWeaponDamage());
        this.defenseStats = new DefenseStats(getName(), stats);

        if (ham.isIncapacitated()) {
            self().tell(new SystemMessage("You have become incapacitated."), self());
        }
    }

    private void applyAttack(ApplyAttack attack) {
        if (ham.isIncapacitated()) {
            sender().tell(new AttackFailed("Target is already incapacitated."), self());
            return;
        }

        final CreatureStats defendedDamage = applyDefenses(attack.getDamage());
        final CreatureStats appliedAttack = ham.subtract(defendedDamage);

        this.setHam(appliedAttack);

        sender().tell(new AttackApplied(ham, defendedDamage), self());
    }

    private CreatureStats applyDefenses(CreatureStats damage) {
        //defend the attack.
        //for now, nothing, so just returning the damage.
        return damage;
    }

    private void attackFailed(AttackFailed msg) {
        self().tell(new SystemMessage(msg.getReason()), self());
    }

    private void sendAttackStats(AttackStatsRequest msg) {
        sender().tell(attackStats, self());
    }

    private void sendDefenseStats(DefenseStatsRequest msg) {
        sender().tell(defenseStats, self());
    }

    private void combatSpam(CombatSpam combatSpam) {
        //System.out.println(String.format("%s combat spams: %s", getName(), combatSpam.toString()));
    }

    private void systemMessage(SystemMessage systemMessage) {
        //System.out.println(String.format("%s system message: %s", getName(), systemMessage.getMessage()));
    }
}