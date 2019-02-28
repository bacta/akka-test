package io.bacta.escrow;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.bacta.chat.MailMessage;
import io.bacta.chat.SystemMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InterplanetaryEscrowServiceActor extends AbstractActor {
    private static final int PENDING_TRANSACTIONS_BUCKET_SIZE = 10000000;
    private static final String MAIL_MESSAGE_FROM = "Interplanetary Escrow Services";
    private static final AtomicLong transactionIdGenerator = new AtomicLong();

    public static Props props() {
        return Props.create(InterplanetaryEscrowServiceActor.class, InterplanetaryEscrowServiceActor::new);
    }

    private final Map<Long, EscrowTransactionActorBased> pendingTransactions;

    private InterplanetaryEscrowServiceActor() {
        this.pendingTransactions = new ConcurrentHashMap<>(PENDING_TRANSACTIONS_BUCKET_SIZE);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartTransaction.class, this::startTransaction)
                .match(WithdrawalAck.class, this::withdrawalAcknowledged)
                .match(WithdrawalFailed.class, this::withdrawalFailed)
                .match(DepositAck.class, this::depositAcknowledged)
                .match(DepositFailed.class, this::depositFailed)
                .build();
    }

    private void startTransaction(StartTransaction message) {
        final EscrowTransactionActorBased transaction = createTransaction(message);

        //Tell the source we wish to withdraw funds into escrow.
        final WithdrawalReq withdrawal = transaction.toWithdrawalWithAcknowledgement();
        transaction.getSource().getActor().tell(withdrawal, self());
    }

    /**
     * Receipt of funds being withdrawn into escrow. Now we can send a request
     * to the target to deposit the funds into their account. If this process fails, then we
     * will refund the amount back to the target.
     *
     * @param message The acknowledgement that funds have been withdrawn.
     */
    private void withdrawalAcknowledged(WithdrawalAck message) {
        final EscrowTransactionActorBased transaction = pendingTransactions.get(message.getTransactionId());
        transaction.getSource().setName(message.getName());

        final DepositReq deposit = transaction.toDepositWithAcknowledgement();
        transaction.getTarget().getActor().tell(deposit, self());
    }

    /**
     * Withdrawal of funds into escrow has failed. Cleanup the transaction, and inform our supervisor that the
     * transaction failed.
     *
     * @param message The acknowledgement that funds failed to be withdrawn.
     */
    private void withdrawalFailed(WithdrawalFailed message) {
        final EscrowTransactionActorBased transaction = pendingTransactions.get(message.getTransactionId());
        transaction.getSource().setName(message.getName());

        final TransferFailed cleanupMessage = new TransferFailed(transaction.getId(), message.getReason(), false);
        cleanupTransaction(transaction, cleanupMessage);
    }

    /**
     * Received acknowledgement by the target that they successfully received the funds. Now we can finish up
     * the transaction by notifying both parties and our supervisor of the successful transfer.
     *
     * @param message The acknowledgement that funds were deposited.
     */
    private void depositAcknowledged(DepositAck message) {
        final EscrowTransactionActorBased transaction = pendingTransactions.get(message.getTransactionId());
        transaction.getTarget().setName(message.getName());

        finishTransaction(transaction);
    }

    /**
     * Deposit of funds into the target's account failed. We need to issue a refund to the source party, and
     * inform our supervisor that the transaction failed.
     *
     * @param message The acknowledgement that funds failed to deposit.
     */
    private void depositFailed(DepositFailed message) {
        final EscrowTransactionActorBased transaction = pendingTransactions.get(message.getTransactionId());
        transaction.getTarget().setName(message.getName());

        issueRefund(transaction, message.getReason());
    }

    /**
     * The transaction was successful. We notify the two parties and our supervisor.
     *
     * @param transaction The transaction that is to be finished.
     */
    private void finishTransaction(EscrowTransactionActorBased transaction) {
        final Party source = transaction.getSource();
        final Party target = transaction.getTarget();

        //The transaction involved a cash tip, then we send tip system messages.
        //What about secure trade? Does that go through a different process, or do we adapt this to handle secure trades.
        if (transaction.getCash() > 0) {
            final String sourceMessage = String.format("You successfully tipped %s %d credits.",
                    target.getName(), transaction.getCash());

            final String targetMessage = String.format("%s tipped you %s credits.",
                    source.getName(), transaction.getCash());

            source.getActor().tell(new SystemMessage(sourceMessage), self());
            target.getActor().tell(new SystemMessage(targetMessage), self());
        }

        if (transaction.getBank() > 0) {
            final String sourceMessage = String.format("Your transfer of %d credits to %s has been successfully delivered via bank wire.",
                    transaction.getBank(), target.getName());

            final String targetMessage = String.format("%d credits have been transferred from %s via bank wire.",
                    transaction.getBank(), source.getName());

            final String subject = "Funds Transferred Successfully";

            source.getActor().tell(new MailMessage(MAIL_MESSAGE_FROM, subject, sourceMessage), self());
            target.getActor().tell(new MailMessage(MAIL_MESSAGE_FROM, subject, targetMessage), self());
        }

        //Tell the parent that we completed a transfer.
        final TransferComplete cleanupMessage = new TransferComplete(transaction.getCash(), transaction.getBank());
        cleanupTransaction(transaction, cleanupMessage);
    }

    /**
     * We are issuing a refund to the source because the target failed to receive the funds for some reason.
     *
     * @param transaction
     * @param reason
     */
    private void issueRefund(EscrowTransactionActorBased transaction, String reason) {
        final String sourceMessage = String.format("Transfer refunded. %s could not receive the funds at this time.",
                transaction.getTarget().getName());

        transaction.getSource().getActor().tell(transaction.toDepositWithoutAcknowledgement(), self());
        transaction.getSource().getActor().tell(new SystemMessage(sourceMessage), self());

        final TransferFailed cleanupMessage = new TransferFailed(transaction.getId(), reason, true);
        cleanupTransaction(transaction, cleanupMessage);
    }

    private void cleanupTransaction(EscrowTransactionActorBased transaction, Object cleanupMessage) {
        context().parent().tell(cleanupMessage, self());
        this.pendingTransactions.remove(transaction.getId());
    }

    private EscrowTransactionActorBased createTransaction(StartTransaction message) {
        final long transactionId = transactionIdGenerator.getAndIncrement();
        final Party source = new Party(message.source);
        final Party target = new Party(message.target);

        final EscrowTransactionActorBased transaction = new EscrowTransactionActorBased(
                transactionId, source, target, message.cash, message.bank);

        this.pendingTransactions.put(transactionId, transaction);

        return transaction;
    }

    @Getter
    @RequiredArgsConstructor
    public static class StartTransaction {
        private final ActorRef source;
        private final ActorRef target;
        private final int cash;
        private final int bank;
    }
}
