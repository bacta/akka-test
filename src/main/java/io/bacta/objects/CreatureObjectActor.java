package io.bacta.objects;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import io.bacta.chat.CombatSpam;
import io.bacta.chat.MailMessage;
import io.bacta.chat.SystemMessage;
import io.bacta.escrow.*;
import io.bacta.messages.creature.SetPropertiesMessage;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Random;

@Getter
@Setter
public class CreatureObjectActor extends TangibleObjectActor {
    private static final Random RNG = new Random();
    private static final int minWeaponDamage = 2;
    private static final int maxWeaponDamage = 36;


    public static Props props(long id, String name) {
        return Props.create(CreatureObjectActor.class, () -> new CreatureObjectActor(id, name));
    }

    private int cash;
    private int bank;

    protected CreatureObjectActor(long id, String name) {
        super(id);

        this.cash = RNG.nextInt();
        this.bank = RNG.nextInt();

        this.setName(name);
    }

    @Override
    protected ReceiveBuilder appendReceiveHandlers(ReceiveBuilder receiveBuilder) {
        return super.appendReceiveHandlers(
                receiveBuilder
                        .match(SetPropertiesMessage.class, this::setProperties)
                        .match(MailMessage.class, this::mailMessage)
                        .match(SystemMessage.class, this::systemMessage)
                        .match(CombatSpam.class, this::combatSpam)
                        .match(WithdrawalReq.class, this::withdraw)
                        .match(DepositReq.class, this::deposit)
        );
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    private void combatSpam(CombatSpam combatSpam) {
        //System.out.println(String.format("%s combat spams: %s", getName(), combatSpam.toString()));
    }

    private void systemMessage(SystemMessage systemMessage) {
        //System.out.println(String.format("%s system message: %s", getName(), systemMessage.getMessage()));
    }

    private void mailMessage(MailMessage mailMessage) {
        //System.out.println(String.format("%s mail: %s with subject %s", getName(), mailMessage.getFrom(), mailMessage.getSubject()));
        //System.out.println(mailMessage.getMessage());
    }

    private void setProperties(SetPropertiesMessage message) {
        for (final String key : message.keySet()) {
            try {
                final Field field = getClass().getDeclaredField(key);
                final Object value = message.get(key);
                field.set(this, value);

                //System.out.printf("Set field '%s' to value %s.\n", key, value);
            } catch (NoSuchFieldException ex) {
                System.out.printf("Tried to set field '%s', but it doesn't exist.\n", key);
            } catch (IllegalAccessException ex) {
                System.out.printf("Tried to set field '%s', but not allowed access.\n", key);
            }
        }
    }

    private void withdraw(WithdrawalReq msg) {
        if (cash < msg.getCash() || bank < msg.getBank()) {
            final WithdrawalFailed failMessage
                    = new WithdrawalFailed(msg.getTransactionId(), "Insufficient funds available.", getName());

            sender().tell(failMessage, self());
            return;
        }

        cash -= msg.getCash();
        bank -= msg.getBank();

        if (msg.isAck()) {
            final WithdrawalAck ack = msg.toAck(getName());
            sender().tell(ack, self());
        }
    }

    private void deposit(DepositReq msg) {
        final long targetCash = cash + msg.getCash();
        final long targetBank = bank + msg.getBank();

        if (targetCash > Integer.MAX_VALUE || targetBank > Integer.MAX_VALUE) {
            final DepositFailed failMessage
                    = new DepositFailed(
                    msg.getTransactionId(),
                    "Insufficient storage for funds. Value would be truncated.",
                    getName());
            sender().tell(failMessage, self());
            return;
        }

        cash += msg.getCash();
        bank += msg.getBank();

        if (msg.isAck()) {
            final DepositAck ack = msg.toAck(getName());
            sender().tell(ack, self()); //Just resent the msg back to the sender.
        }
    }
}