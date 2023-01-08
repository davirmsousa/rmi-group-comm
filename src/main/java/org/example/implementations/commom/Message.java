package org.example.implementations.commom;

import org.example.constants.Constants;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    public static final String SERVER_RESPONSE_SUBJECT = "serverResponse";
    public static final String CLIENT_REQUEST_SUBJECT = "clientRequest";
    public static final String REPLICATE_REQUEST_SUBJECT = "replicate";
    public static final String ACCEPT_NEW_MEMBER_SUBJECT = "accNewMember";
    public static final String REJECT_NEW_MEMBER_SUBJECT = "regNewMember";
    public static final String REQUEST_JOIN_GROUP_SUBJECT = "joinGroup";
    public static final String IGNORED_SUBJECT = "ignored";
    public static final String UNANSWERED_SUBJECT = "unanswered";

    private String fromRoute = Constants.SERVER_REGISTY_BOUND_BASE_NAME;
    private String route = Constants.SERVER_REGISTY_BOUND_BASE_NAME;
    private UUID id;
    private String subject;
    private String message;
    private Long senderId;
    private Long recipientId;

    private Message answeredMessage;

    public Message(Long senderId, String subject) {
        if (senderId != null && senderId < 0)
            throw new IllegalArgumentException("'senderId' precisa ter um valor valido");

        if (subject == null || subject.trim().equals(""))
            throw new IllegalArgumentException("'subject' eh obrigatorio");

        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.recipientId = null;
        this.subject = subject;
        this.message = null;
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Message getAnsweredMessage() {
        return answeredMessage;
    }

    public void setAnsweredMessage(Message answeredMessage) {
        this.answeredMessage = answeredMessage;
    }

    public String getFromRoute() {
        return fromRoute;
    }

    public void setFromRoute(String fromRoute) {
        this.fromRoute = fromRoute;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Message clone() {
        Message clone = new Message(senderId, subject);
        clone.answeredMessage = answeredMessage;
        clone.recipientId = recipientId;
        clone.message = message;
        clone.id = id;
        return clone;
    }
}
