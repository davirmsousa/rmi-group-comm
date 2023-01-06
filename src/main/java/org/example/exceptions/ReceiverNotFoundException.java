package org.example.exceptions;

public class ReceiverNotFoundException extends Exception {

    public ReceiverNotFoundException() {
        super();
    }

    public ReceiverNotFoundException(String message) {
        super(message);
    }
}
