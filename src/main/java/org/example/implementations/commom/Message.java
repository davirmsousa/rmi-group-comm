package org.example.implementations.commom;

import java.io.Serializable;

public class Message implements Serializable {
    public static final String CLIENT_REQUEST_SUBJECT = "request";
    public static final String REPLICATE_REQUEST_SUBJECT = "replicate";
    public static final String ACCEPT_NEW_MEMBER_SUBJECT = "accNewMember";
    public static final String REJECT_NEW_MEMBER_SUBJECT = "regNewMember";
    public static final String REQUEST_JOIN_GROUP_SUBJECT = "joinGroup";
    public static final String IGNORED_SUBJECT = "ignored";
    public static final String UNANSWERED_SUBJECT = "unanswered";

    private final String subject;
    private String message;
    private final Long senderId;

    private SCompletableFuture<Message> responseChannel;

    public Message(Long senderId, String subject) {
        if (senderId != null && senderId < 0)
            throw new IllegalArgumentException("'senderId' precisa ter um valor valido");

        if (subject == null || subject.trim().equals(""))
            throw new IllegalArgumentException("'subject' eh obrigatorio");

        this.senderId = senderId;
        this.subject = subject;
        this.message = null;
    }

    public Message(Long senderId, String subject, String message) {
        this(senderId, subject);

        this.message = message;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setResponseChannel(SCompletableFuture<Message> responseChannel) {
        this.responseChannel = responseChannel;
    }

    public void reply(Message message) {
        this.responseChannel.complete(message);
    }

    public Message clone() {
        return new Message(senderId, subject, message);
    }
}
