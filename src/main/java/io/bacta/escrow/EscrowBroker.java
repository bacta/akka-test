package io.bacta.escrow;

import akka.actor.AbstractActor;
import akka.actor.Props;
import io.bacta.chat.MailMessage;
import io.bacta.chat.SystemMessage;
import lombok.Getter;

@Getter
public class EscrowBroker extends AbstractActor {
    public static Props props(Party source, Party target, int cash, int bank) {
        return Props.create(EscrowBroker.class, () -> new EscrowBroker(source, target, cash, bank));
    }

    private static final String escrowMailAccount = "Planetary Financial Escrow Services";

    private final Party source;
    private final Party target;

    private final int cash;
    private final int bank;

    private EscrowBroker(Party source, Party target, int cash, int bank) {
        this.source = source;
        this.target = target;

        this.cash = cash;
        this.bank = bank;

        //Start things off with a withdrawal request to the source.
        if (cash > 0 || bank > 0) {
            source.getActor().tell(new Withdrawal(cash, bank), self());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Withdrawal.class, this::withdrawn)
                .match(WithdrawalFailed.class, this::withdrawalFailed)
                .match(Deposit.class, this::deposited)
                .match(DepositFailed.class, this::depositFailed)
                .build();
    }

    private void withdrawn(Withdrawal msg) {
        //We've received the valuables from the source, so now we can continue to see if the target can receive them.
        target.getActor().tell(msg.toDeposit(), self());
    }

    private void deposited(Deposit msg) {
        //We received notification that the target received the funds, so now we can tell the source that the transfer
        //was a success.

        //If cash credits were part of the transfer, inform the parties.
        if (msg.getCash() > 0) {
            source.getActor().tell(new SystemMessage(String.format("You successfully tipped your %s %d credits.", target.getName(), msg.getCash())), self());
            target.getActor().tell(new SystemMessage(String.format("%s tipped you %d credits.", source.getName(), msg.getCash())), self());
        }

        //If bank credits were part of the transfer, send mail notifications.
        if (msg.getBank() > 0) {
            final String sourceSubject = "Transfer Successful";
            final String sourceMessage = String.format("You successfully transferred %d credits to %s.", msg.getBank(), target.getName());

            final String targetSubject = "Received Credit Transfer";
            final String targetMessage = String.format("You received %d credits via bank wire transfer from %s.", msg.getBank(), source.getName());

            source.getActor().tell(new MailMessage(escrowMailAccount, sourceSubject, sourceMessage), self());
            target.getActor().tell(new MailMessage(escrowMailAccount, targetSubject, targetMessage), self());
        }

        //TODO: If items were part of the transfer, notify...

        //And we're done. Tell the escrow company so they can log it to the books.
        context().parent().tell(new TransferComplete(cash, bank), self());
    }

    private void withdrawalFailed(WithdrawalFailed msg) {
        source.getActor().tell(new SystemMessage(msg.getMessage()), self());
        context().parent().tell(new TransferFailed(false), self());
    }

    private void depositFailed(DepositFailed msg) {
        target.getActor().tell(new SystemMessage(msg.getMessage()), self());
        issueRefund();
        context().parent().tell(new TransferFailed(true), self());
    }

    private void issueRefund() {
        source.getActor().tell(new Deposit(cash, bank), self());
        source.getActor().tell(new SystemMessage("Transfer failed. Funds have been refunded."), self());
    }
}
