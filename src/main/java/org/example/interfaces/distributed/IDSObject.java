package org.example.interfaces.distributed;

import org.example.implementations.commom.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDSObject extends Remote {
    /** Aguarda o recebimento de uma mensagem */
    void receiveMessage(Message message) throws RemoteException;

    /** Retorna o UUID */
    long getId() throws RemoteException;
}
