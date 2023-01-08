package org.example.implementations.client;

import org.example.Main;
import org.example.constants.Constants;
import org.example.helpers.RemoteHelper;
import org.example.implementations.commom.Message;
import org.example.implementations.commom.ResultsCollector;
import org.example.interfaces.client.IClient;
import org.example.interfaces.client.IDSClient;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Client extends UnicastRemoteObject implements IClient, IDSClient {
    private long id;

    public Client() throws AlreadyBoundException, RemoteException {
        this.registerRemotely();
    }

    private void registerRemotely() throws RemoteException, AlreadyBoundException {
        Registry registry = Main.getRegistry();

        // pegar a lista de clientes registrados
        List<String> registryBindedNames = RemoteHelper
                .getRegistryBindedNameByKey(registry, Constants.CLIENT_REGISTY_BOUND_BASE_NAME);
        System.out.println("[Client | " + this.id + "] got registered clients list: " + registryBindedNames.size());

        // obter o id do cliente na lista
        this.id = RemoteHelper.getNewItemId(registryBindedNames);
        System.out.println("[Client | " + this.id + "] got new id");

        // cria o nome e registrar o servidor
        String clientBindName = Constants.CLIENT_REGISTY_BOUND_BASE_NAME + this.id;
        registry.bind(clientBindName, this);
        System.out.println("[Client | " + this.id + "] client registered in '" + clientBindName + "'");
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public void receiveMessage(Message message) {
        System.out.println("[CLIENT | " + this.id +"] receiveMessage: " + message.getSubject() + " -- " + message.getMessage());
    }

    public void handleUserInteraction() throws RemoteException, NotBoundException {
        Message message = new Message(this.id, Message.CLIENT_REQUEST_SUBJECT);
        message.setMessage("insert");
        message.setFromRoute(Constants.CLIENT_REGISTY_BOUND_BASE_NAME);

        RemoteHelper.sendMessage(message, new ResultsCollector<>());
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, AlreadyBoundException {
        Client client = new Client();
        client.handleUserInteraction();
    }
}
