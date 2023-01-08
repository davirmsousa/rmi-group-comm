package org.example.implementations.commom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ResultsCollector<T> {
    private final SCompletableFuture<T> future;
    private final List<T> sentMessages;
    private final List<T> receivedAcks;

    public ResultsCollector() {
        this.future = new SCompletableFuture<>();
        this.sentMessages = new ArrayList<>();
        this.receivedAcks = new ArrayList<>();
    }

    public void ackReceived(T ack, Function<List<T>, T> whenReceivedAllAcks) {
        receivedAcks.add(ack);

        if (receivedAcks.size() == sentMessages.size()) {
            T reducedResult = whenReceivedAllAcks.apply(receivedAcks);
            complete(reducedResult);
        }
    }

    public void messageSent(T message) {
        sentMessages.add(message);
    }

    public void orTimeout(int time, TimeUnit unit) {
        future.orTimeout(time, unit);
    }

    public void whenComplete(BiConsumer<? super T, ? super Throwable> func) {
        future.whenComplete(func);
    }

    public void complete(T result) {
        future.complete(result);
    }

    public T get() throws ExecutionException, InterruptedException { return future.get(); }

    public static ResultsCollector<Boolean> getCompleted() {
        ResultsCollector<Boolean> completedCollector = new ResultsCollector();
        completedCollector.complete(true);
        return completedCollector;
    }
}
