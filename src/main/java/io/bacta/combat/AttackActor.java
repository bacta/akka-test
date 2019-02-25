package io.bacta.combat;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.bacta.chat.CombatSpam;
import io.bacta.chat.SystemMessage;
import io.bacta.sim.BattleSimulationActor;

import java.util.List;

public class AttackActor extends AbstractActor {
    public static Props props(ActorRef attacker, ActorRef defender, AttackTypes.Attack attack) {
        return Props.create(AttackActor.class, () -> new AttackActor(attacker, defender, attack));
    }

    private final ActorRef attacker;
    private final ActorRef defender;
    private final AttackTypes.Attack attack;

    private AttackStats attackStats;
    private DefenseStats defenseStats;

    public AttackActor(ActorRef attacker, ActorRef defender, AttackTypes.Attack attack) {
        this.attacker = attacker;
        this.defender = defender;
        this.attack = attack;

        attacker.tell(new AttackStatsRequest(), self());
        defender.tell(new DefenseStatsRequest(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AttackStats.class, this::attackStats)
                .match(DefenseStats.class, this::defenseStats)
                .match(AttackFailed.class, this::attackFailed)
                .match(AttackApplied.class, this::attackApplied)
                .build();
    }

    private void applyAttack() {
        if (!this.ready())
            return;

        //If the attacker is already incapacitated, then we can stop the attack.
        if (attackStats.getHam().isIncapacitated()) {
            attacker.tell(new AttackFailed("You may not attack while incapacitated."), self());
            sendIncapacitatedParticipant(attacker);
            context().stop(self());
            return;
        }

        //Before we even try to apply an attack, we check if the defender is even able to be attacked.
        if (defenseStats.getHam().isIncapacitated()) {
            attacker.tell(new AttackFailed(String.format("%s is already incapacitated.", defenseStats.getName())), self());
            sendIncapacitatedParticipant(defender);
            context().stop(self());
            return;
        }

        final CreatureStats attackerHam = attackStats.getHam();
        final CreatureStats attackCost = attack.getCost();
        final CreatureStats attackDelta = attackerHam.subtract(attackCost);

        final boolean wouldIncapacitateAttacker = attackDelta.isIncapacitated();

        if (wouldIncapacitateAttacker) {
            attacker.tell(new AttackFailed("You do not have enough stats to perform the attack."), self());
            context().stop(self());
            return;
        }


        final int weaponDamage = attackStats.getWeaponDamage();
        final CreatureStats attackDamage = attack.getDamage().multiplyScalar(weaponDamage);

        defender.tell(new ApplyAttack(attackDamage), self());
    }

    private void attackApplied(AttackApplied attackApplied) {

        final List<CombatSpam> spam = attackApplied.getAppliedDamage().createCombatSpam(
                attackStats.getName(),
                defenseStats.getName(),
                attack.getName());

        spam.forEach(s -> attacker.tell(s, self()));

        if (attackApplied.getHam().isIncapacitated()) {
            attacker.tell(new SystemMessage(String.format("You have incapacitated %s.", defenseStats.getName())), self());
            sendIncapacitatedParticipant(defender);
        }

        context().stop(self());
    }

    private void attackFailed(AttackFailed attackFailed) {
        attacker.forward(attackFailed, context());
        sendIncapacitatedParticipant(defender);

        context().stop(self());
    }

    private void attackStats(AttackStats attackStats) {
        this.attackStats = attackStats;
        this.applyAttack();
    }

    private void defenseStats(DefenseStats defenseStats) {
        this.defenseStats = defenseStats;
        this.applyAttack();
    }

    private void sendIncapacitatedParticipant(ActorRef participant) {
        context().parent().tell(new BattleSimulationActor.ParticipantIncapacitated(participant), self());
    }

    private boolean ready() {
        return this.attackStats != null && this.defenseStats != null;
    }
}
