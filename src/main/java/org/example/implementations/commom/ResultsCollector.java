package org.example.implementations.commom;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ResultsCollector<T> {
    private int expectedAmountMessagensToSend;
    private final List<T> sentMessages;
    private final List<T> receivedAcks;
    private Function<List<T>, T> whenReceivedAllAcks;
    private BiFunction<ResultsCollector<T>, T, T> whenReceivedAck;

    private UUID originalMessageId;
    private T result;

    public ResultsCollector() {
        this.receivedAcks = new ArrayList<>();
        this.sentMessages = new ArrayList<>();
        this.whenReceivedAllAcks = null;
        this.result = null;
    }

    public ResultsCollector(BiFunction<ResultsCollector<T>, T, T> whenReceivedAck, Function<List<T>, T> whenReceivedAllAcks) {
        this();
        this.whenReceivedAllAcks = whenReceivedAllAcks;
        this.whenReceivedAck = whenReceivedAck;
    }

    public void ackReceived(T ack) {
        receivedAcks.add(ack);

        if (whenReceivedAck != null)
            whenReceivedAck.apply(this, ack);

        if (isDone() && whenReceivedAllAcks != null) {
            result = whenReceivedAllAcks.apply(receivedAcks);
        }
    }

    public void messageSent(T message) {
        sentMessages.add(message);
    }

    public T getResult() { return result; }

    public void complete(T result) {
        this.result = result;
    }

    public Boolean isDone() {
        return (
            receivedAcks.size() == sentMessages.size()) &&
            (sentMessages.size() == expectedAmountMessagensToSend);
    }

    public void setExpectedAmountMessagensToSend(int expectedAmountMessagensToSend) {
        this.expectedAmountMessagensToSend = expectedAmountMessagensToSend;
    }

    public UUID getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(UUID originalMessageId) {
        this.originalMessageId = originalMessageId;
    }
}
