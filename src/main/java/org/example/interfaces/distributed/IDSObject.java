package org.example.interfaces.distributed;

import org.example.implementations.commom.Message;

import java.util.UUID;

public interface IDSObject {
    /** Aguarda o recebimento de uma mensagem */
    void receiveMessage(Message message);

    /** Retorna o UUID */
    UUID getId();
}
