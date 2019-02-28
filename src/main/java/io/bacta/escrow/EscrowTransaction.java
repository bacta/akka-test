package io.bacta.escrow;

import io.bacta.chat.MailMessage;
import io.bacta.objects.CreatureObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This is the non actor based version of an escrow transaction.
 */
@Getter
@RequiredArgsConstructor
public class EscrowTransaction {
    private static final String escrowMailAccount = "Planetary Financial Escrow Services";

    private final CreatureObject source;
    private final CreatureObject target;
    private final int cash;
    private final int bank;

    private boolean refundIssued;
    private boolean failed;

    public void performTransfer() throws InsufficientFundsStorageException {
        try {
            source.withdraw(cash, bank); //We could even wrap the above up in an exception being thrown by withdraw.
            target.deposit(cash, bank);

            //If cash credits were part of the transfer, inform the parties.
            if (cash > 0) {
                source.sendSystemMessage(String.format("You successfully tipped %s %d credits.", target.getName(), cash));
                target.sendSystemMessage(String.format("%s tipped you %d credits.", source.getName(), cash));
            }

            //If bank credits were part of the transfer, send mail notifications.
            if (bank > 0) {
                final String sourceSubject = "Transfer Successful";
                final String sourceMessage = String.format("You successfully transferred %d credits to %s.", bank, target.getName());

                final String targetSubject = "Received Credit Transfer";
                final String targetMessage = String.format("You received %d credits via bank wire transfer from %s.", bank, source.getName());

                source.sendMailMessage(new MailMessage(escrowMailAccount, sourceSubject, sourceMessage));
                target.sendMailMessage(new MailMessage(escrowMailAccount, targetSubject, targetMessage));
            }

        } catch (InsuffientFundsException ex) {
            this.failed = true;

            source.sendSystemMessage(ex.getMessage());
        } catch (InsufficientFundsStorageException ex) {
            this.failed = true;

            target.sendSystemMessage(ex.getMessage());
            issueRefund();
        }
    }

    private void issueRefund() throws InsufficientFundsStorageException {
        source.deposit(cash, bank);
        source.sendSystemMessage("Transfer failed. Funds have been refunded.");

        this.refundIssued = true;
    }
}
