package io.bacta.escrow;

public class InsufficientFundsStorageException extends Exception {
    public InsufficientFundsStorageException() {
        super("Insufficient storage for funds. Value would be truncated.");
    }
}
