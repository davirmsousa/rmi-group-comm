package org.example.implementations.commom;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    public static final String REPLY_SUBJECT = "reply";
    public static final String CLIENT_REQUEST_SUBJECT = "request";
    public static final String HEALTHCHECK_SUBJECT = "request";
    public static final String ACCEPT_NEW_MEMBER_SUBJECT = "accnewMember";
    public static final String REJECT_NEW_MEMBER_SUBJECT = "regnewMember";
    public static final String REQUEST_JOIN_GROUP_SUBJECT = "joinGroup";

    public String subject;
    public String message;

    public UUID senderId;
    public UUID receiverId;

    public Message(UUID senderId, String subject) {
        if (senderId == null)
            throw new IllegalArgumentException("'senderId' eh obrigatorio");

        if (subject == null || subject.trim().equals(""))
            throw new IllegalArgumentException("'subject' eh obrigatorio");

        this.senderId = senderId;
        this.subject = subject;
        this.message = null;
    }

    public Message(UUID senderId, String subject, String message) {
        this(senderId, subject);

        this.message = message;
    }

}
