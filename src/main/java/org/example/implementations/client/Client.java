package org.example.implementations.client;

import org.example.constants.Constants;
import org.example.implementations.commom.Message;
import org.example.interfaces.client.IClient;
import org.example.interfaces.client.IDSClient;
import org.example.interfaces.coordinator.IClientCoordinator;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client implements IClient, IDSClient {
    private long id;

    public Client(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public void receiveMessage(Message message) {
        System.out.println("[CLIENT | " + this.id +"] receiveMessage: " + message.getSubject() + " -- " + message.getMessage());
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {
        System.out.println("[Client|Main] trying to get registry");
        Registry registry = LocateRegistry.getRegistry(Constants.DEFAULT_PORT);

        System.out.println("[Client|Main] trying to get coordinator");
        IClientCoordinator clientCoordinator =
                (IClientCoordinator) registry.lookup(Constants.COORDINATOR_REGISTRY_NAME);

        Client client = new Client(0);

        System.out.println("[Client|Main] sending message 1");
        clientCoordinator.sendBroadcastMessage(
            new Message(client.getId(), Message.CLIENT_REQUEST_SUBJECT, "Client Request1")
        );

        System.out.println("[Client|Main] sending message 2");
        clientCoordinator.sendBroadcastMessage(
            new Message(client.getId(), Message.CLIENT_REQUEST_SUBJECT, "Client Request2")
        );

        System.out.println("[Client|Main] sending message 3");
        clientCoordinator.sendBroadcastMessage(
            new Message(client.getId(), Message.CLIENT_REQUEST_SUBJECT, "Client Request3")
        );

        System.out.println("[Client|Main] all messages sent");

        while(!Thread.currentThread().isInterrupted());
    }
}
