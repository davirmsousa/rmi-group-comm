package org.example.interfaces.coordinator;

import org.example.implementations.commom.Message;
import org.example.interfaces.client.IDSClient;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientCoordinator extends Remote {
    /**
     * Envia uma mensagem para ser executada pelo servidor
     * @param message mensagem que deve ser executada
     */
    void sendBroadcastMessage(Message message) throws RemoteException;

    /** Registra o cliente para que possa receber respostas do servidor */
    void registerClient(IDSClient client) throws RemoteException;
}
