package io.bacta.objects;

import io.bacta.chat.MailMessage;
import io.bacta.escrow.InsufficientFundsStorageException;
import io.bacta.escrow.InsuffientFundsException;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
@Setter
public class CreatureObject extends TangibleObject {
    private static final Random RNG = new Random();

    private String name;

    private int cash = RNG.nextInt();
    private int bank = RNG.nextInt();

    public CreatureObject(String name) {
        this.name = name;
    }

    public void sendSystemMessage(String msg) {
        //System.out.printf("%s system message: %s\n", name, msg);
    }

    public void sendMailMessage(MailMessage mailMessage) {
        //System.out.println(String.format("%s mail: %s with subject %s", getName(), mailMessage.getFrom(), mailMessage.getSubject()));
        //System.out.println(mailMessage.getMessage());
    }

    public void deposit(int cash, int bank) throws InsufficientFundsStorageException {
        final long targetCash = this.cash + cash;
        final long targetBank = this.bank + bank;

        if (targetCash > Integer.MAX_VALUE || targetBank > Integer.MAX_VALUE) {
            throw new InsufficientFundsStorageException();
        }

        this.cash += cash;
        this.bank += bank;
    }

    public void withdraw(int cash, int bank) throws InsuffientFundsException {
        if (this.cash < cash || this.bank < bank) {
            throw new InsuffientFundsException();
        }

        this.cash -= cash;
        this.bank -= bank;
    }
}
