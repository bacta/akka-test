package io.bacta.escrow;

public class InsuffientFundsException extends Exception {
    public InsuffientFundsException() {
        super("Insufficient funds available.");
    }
}
