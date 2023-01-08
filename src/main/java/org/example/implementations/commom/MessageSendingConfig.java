package org.example.implementations.commom;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MessageSendingConfig {
    private Integer timeout = null;
    private TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;

    Function<ResultsCollector<Message>, BiConsumer<? super Message, ? super Throwable>> whenMessageComplete = null;

    BiConsumer<? super Message, ? super Throwable> whenCollectorComplete = null;

    public MessageSendingConfig setTimeout(int time) {
        this.timeout = time;
        return this;
    }

    public MessageSendingConfig setTimeoutUnit(TimeUnit unit) {
        this.timeoutUnit = unit;
        return this;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public MessageSendingConfig setWhenMessageComplete(Function<ResultsCollector<Message>, BiConsumer<? super Message, ? super Throwable>> whenComplete) {
        this.whenMessageComplete = whenComplete;
        return this;
    }

    public BiConsumer<? super Message, ? super Throwable> getWhenMessageComplete(ResultsCollector<Message> collector) {
        return whenMessageComplete.apply(collector);
    }

    public MessageSendingConfig setWhenCollectorComplete(BiConsumer<? super Message, ? super Throwable> whenComplete) {
        this.whenCollectorComplete = whenComplete;
        return this;
    }

    public BiConsumer<? super Message, ? super Throwable> getWhenCollectorComplete() {
        return whenCollectorComplete;
    }
}
