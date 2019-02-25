package io.bacta.sim;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import io.bacta.combat.AttackActor;
import io.bacta.combat.AttackTypes;
import io.bacta.combat.CreatureStats;
import io.bacta.name.NameGenerator;
import io.bacta.objects.CreatureObjectActor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BattleSimulationActor extends AbstractActor {
    private static final AtomicInteger battleIdGenerator = new AtomicInteger();
    private static final Random RNG = new Random();

    private final AtomicInteger battleRound = new AtomicInteger();

    public static Props props(int totalParticipants) {
        return Props.create(BattleSimulationActor.class, () -> new BattleSimulationActor(totalParticipants));
    }

    private final int battleId;
    private final List<BattleParticipant> participants;

    public BattleSimulationActor(int totalParticipants) {
        this.battleId = battleIdGenerator.incrementAndGet();

//        System.out.printf("Battle %d created with %d participants.\n", battleId, totalParticipants);

        this.participants = Stream.generate(this::createParticipant)
                .limit(totalParticipants)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartBattle.class, this::startBattle)
                .match(FinishBattle.class, this::finishBattle)
                .match(StartBattleRound.class, this::startBattleRound)
                .match(AttackRandomParticipant.class, this::attackRandomParticipant)
                .match(ParticipantIncapacitated.class, this::participantIncapacitated)
                .build();
    }

    private void startBattle(StartBattle msg) {
        self().tell(new StartBattleRound(), self());
    }

    private void startBattleRound(StartBattleRound msg) {
        final Materializer materializer = ActorMaterializer.create(context().system());

        final List<BattleParticipant> activeParticipants = participants.stream()
                .filter(p -> !p.incapacitated)
                .collect(Collectors.toList());

        if (activeParticipants.size() <= 1) {
            self().tell(new FinishBattle(), self());
            return;
        }

        battleRound.incrementAndGet();

//        System.out.printf("Starting battle round %d with %d active participants.\n",
//                battleRound.get(),
//                activeParticipants.size());

        Source.from(activeParticipants)
                .map(AttackRandomParticipant::new)
                .runForeach(m -> self().tell(m, self()), materializer)
                .thenRun(() -> self().tell(new StartBattleRound(), self()));
    }

    private void finishBattle(FinishBattle msg) {
        final List<String> winningParticipants = participants.stream()
                .filter(p -> !p.incapacitated)
                .map(p -> p.name)
                .collect(Collectors.toList());

        final BattleSimulationResult result
                = new BattleSimulationResult(battleId, battleRound.get(), participants.size(), winningParticipants);

        context().parent().tell(result, self());
    }

    private void participantIncapacitated(ParticipantIncapacitated msg) {
        for (final BattleParticipant participant : participants) {
            if (participant.participant.equals(msg.participant)) {
                participant.incapacitated = true;
            }
        }
    }

    private void attackRandomParticipant(AttackRandomParticipant msg) {
        final ActorRef attacker = msg.getParticipant().participant;
        final ActorRef defender = chooseRandomParticipant(attacker);
        final AttackTypes.Attack attack = AttackTypes.chooseRandomAttack();

        if (defender == null) {
            //Unable to find any defenders, so battle is over...
            return;
        }

        //Creating an actor to carry out the attack.
        context().actorOf(AttackActor.props(attacker, defender, attack));
    }

    private ActorRef chooseRandomParticipant(ActorRef attacker) {
        final List<BattleParticipant> activeParticipants = participants.stream()
                .filter(p -> !p.incapacitated)
                .filter(p -> !p.participant.equals(attacker))
                .collect(Collectors.toList());

        final int count = activeParticipants.size();

        if (count < 1)
            return null;

        if (count == 1)
            return activeParticipants.get(0).participant;

        //Otherwise, pick a random one.
        final int index = RNG.nextInt(activeParticipants.size() - 1);
        //Concurrency issue here as participants size could change after we fetched it to generate the random index...
        return activeParticipants.get(index).participant;
    }

    private BattleParticipant createParticipant() {
        final CreatureStats stats = CreatureStats.random();
        final String name = NameGenerator.generate();

        //System.out.printf("Created participant %s (%s)\n", name, stats.toString());

        final ActorRef participant = context().actorOf(CreatureObjectActor.props(stats, name));

        return new BattleParticipant(participant, name);
    }

    public static class StartBattle {
    }

    private static class StartBattleRound {
    }

    private static class FinishBattle {
    }

    @Getter
    @RequiredArgsConstructor
    public class BattleParticipant {
        private final ActorRef participant;
        private final String name;

        @Setter
        private boolean incapacitated;
    }

    @Getter
    @RequiredArgsConstructor
    public static class AttackRandomParticipant {
        private final BattleParticipant participant;
    }

    /**
     * Just using this for the simulation so that we can stop incapacitated participants from trying to attack.
     */
    @Getter
    @RequiredArgsConstructor
    public static class ParticipantIncapacitated {
        private final ActorRef participant;
    }
}
