package org.example.interfaces.coordinator;

import org.example.implementations.commom.Message;
import org.example.interfaces.server.IDSServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerCoordinator extends Remote {
    /**
     * Envia uma mensagem em broadcast para os membros do grupo
     * @param message mensagem que deve ser executada
     */
    void sendBroadcastMessage(Message message) throws RemoteException;

    /** Responde uma mensagem enviada por outro membro do grupo */
    void replyMessage(Message message) throws RemoteException;

    /** Solicita o registro do servidor dentro do grupo */
    void registerServer(IDSServer server) throws RemoteException;
}
