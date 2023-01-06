package org.example.implementations.coordinator;

import org.example.constants.Constants;
import org.example.implementations.commom.Message;
import org.example.interfaces.client.IDSClient;
import org.example.interfaces.coordinator.IClientCoordinator;
import org.example.interfaces.coordinator.IServerCoordinator;
import org.example.interfaces.distributed.IDSObject;
import org.example.interfaces.server.IDSServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Coordinator extends UnicastRemoteObject implements IServerCoordinator, IClientCoordinator {
    private final ConcurrentHashMap<UUID, IDSClient> clients;
    private final ConcurrentHashMap<UUID, IDSServer> servers;
    private final ConcurrentHashMap<UUID, IDSServer> pendingServers;

    public Coordinator() throws RemoteException {
        super();

        System.out.println("[Coordinator] initializing properties");

        this.pendingServers = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
        this.servers = new ConcurrentHashMap<>();

        System.out.println("[Coordinator] trying to create registry");
        Registry registry = LocateRegistry.createRegistry(Constants.COORDINATOR_REGISTRY_PORT);
        System.out.println("[Coordinator] trying to rebind this coordinator");
        registry.rebind(Constants.COORDINATOR_REGISTRY_NAME, this);
        System.out.println("[Coordinator] construction finished");
    }

    public void sendBroadcastMessage(Message message) throws RemoteException {

        System.out.println("[Coordinator] sendBroadcastMessage: message subject to send: " + message.subject);

        switch (message.subject) {
            case Message.ACCEPT_NEW_MEMBER_SUBJECT:
                // tratamento especial caso o join tenha sido aprovado
                this.handleNewMemberRegistration(message);
                break;
            case Message.REJECT_NEW_MEMBER_SUBJECT:
                // tratamento especial caso o join tenha sido rejeitado
                this.handleNewMemberRejection(message);
                break;
            default:
                break;
        }

        // envia mensagem para todos, exceto para quem mandou
        for (IDSServer server: this.servers.values()) {
            if (server.getId().equals(message.senderId))
                continue;

            System.out.println("[Coordinator] sendBroadcastMessage: sending message to server " +  server.getId());

            try { server.receiveMessage(message); }
            catch (Exception ignored) {} // caso nao consiga enviar a mensagem, nao quebra o restante
        }
    }

    public void replyMessage(Message message) {
        // procura o remetente
        IDSObject receiver = this.findObject(message.receiverId);

        if (receiver == null)
            return;

        // envia a mensagem para o remetente
        receiver.receiveMessage(message);
    }

    public void registerClient(IDSClient client) {
        // valida se o cliente esta no grupo
        if (this.clients.containsKey(client.getId()))
            return;

        // adiciona o cliente na lista
        this.clients.put(client.getId(), client);
    }

    public void registerServer(IDSServer server) {
        System.out.println("[Coordinator] registerServer: validating if there is any server");
        // valida se existe algum servidor no grupo, se nao, o primeiro eh o lider
        if (this.servers.isEmpty()) {
            server.setLeader(true);
            this.servers.put(server.getId(), server);

            System.out.println("[Coordinator] registerServer: server " + server.getId() + " is the new leader");

            return;
        }

        System.out.println("[Coordinator] registerServer: validating if is already registered");
        // valida se o servidor ja esta no grupo
        if (this.servers.containsKey(server.getId()))
            return;

        System.out.println("[Coordinator] registerServer: validating if is join request is pending");
        // valida se o servidor ja pediu para entrar no grupo
        if (this.pendingServers.containsKey(server.getId()))
            return;

        System.out.println("[Coordinator] registerServer: adding in pending list");
        // adiciona na lista de pendentes de validacao
        this.pendingServers.put(server.getId(), server);

        // envia mensagem em broadcast para o lider validar o novo menbro
        try {
            System.out.println("[Coordinator] registerServer: sending request to join");
            this.sendBroadcastMessage(
                new Message(server.getId(), Message.REQUEST_JOIN_GROUP_SUBJECT)
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleNewMemberRegistration(Message message) {
        // pega o id do novo servidor a partir do conteudo da mensagem
        UUID newMemberUUID = UUID.fromString(message.message);

        // valida se ainda esta na lista de espera
        if (!this.pendingServers.containsKey(newMemberUUID))
            return;

        // remove da lista de espera
        IDSServer newember = this.pendingServers.remove(newMemberUUID);

        // adiciona na lista do grupo
        this.servers.put(newMemberUUID, newember);
    }

    private void handleNewMemberRejection(Message message) {
        // pega o id do novo servidor a partir do conteudo da mensagem
        UUID newMemberUUID = UUID.fromString(message.message);

        // remove da lista de espera
        IDSServer rejectedServer = this.pendingServers.remove(newMemberUUID);

        // transmite a mensagem de rejeicao para o servidor rejeitado
        rejectedServer.receiveMessage(message);
    }

    private IDSObject findObject(UUID id) {
        // procura na lista de servidores
        Optional<IDSServer> optServer = this.servers.values().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();

        // retorna o servidor se encontrou
        if (optServer.isPresent()) return optServer.get();

        // procura na lista de clientes
        Optional<IDSClient> optClient = this.clients.values().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();

        // retorna o cliente ou NULL
        return optClient.orElse(null);
    }

    public static void main(String[] args) throws RemoteException { new Coordinator(); }
}
