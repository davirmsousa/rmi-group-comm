package org.example.implementations.server;

import org.example.constants.Constants;
import org.example.implementations.commom.Message;
import org.example.implementations.server.threadRunners.CommandExecutionThreadRunner;
import org.example.interfaces.coordinator.IServerCoordinator;
import org.example.interfaces.server.IDSServer;
import org.example.interfaces.server.IServer;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Server implements IServer, IDSServer, Serializable {
    private final UUID id;
    private Boolean isLeader;
    private final IServerCoordinator remoteCoordinator;
    private final ConcurrentLinkedDeque<Thread> queueCommandsToExecute;

    public Server(IServerCoordinator remoteCoordinator) {
        this.queueCommandsToExecute = new ConcurrentLinkedDeque<>();
        this.remoteCoordinator = remoteCoordinator;
        this.id = UUID.randomUUID();
        this.isLeader = false;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public void receiveMessage(Message message) {
        System.out.println("[SERVER | " + this.id +"] receiveMessage: " + message.subject + " -- " + message.message);

        Boolean shouldIgnoreMessage = this.shouldIgnoreMessage(message);
        System.out.println("[SERVER | " + this.id +"] receiveMessage: shouldIgnoreMessage: " + shouldIgnoreMessage);

        if (shouldIgnoreMessage)
            return;

        switch (message.subject) {
            case Message.REQUEST_JOIN_GROUP_SUBJECT:
                this.handleJoinGroupRequestMessage(message);
                break;
            case Message.CLIENT_REQUEST_SUBJECT:
                this.handleClientRequestMessage(message);
                break;
            default:
                break;
        }
    }

    private Boolean shouldIgnoreMessage(Message message) {
        List<String> leaderSubjects = Arrays.asList(
            Message.CLIENT_REQUEST_SUBJECT,
            Message.REQUEST_JOIN_GROUP_SUBJECT
        );

        List<String> slaveSubjects = Arrays.asList(
            "1", "2"
        );

        boolean imTheReceiver = message.receiverId == null || message.receiverId.equals(this.id);
        boolean subjectOnlyToLeader = leaderSubjects.contains(message.subject) && !this.isLeader;
        boolean subjectOnlyToSlave = slaveSubjects.contains(message.subject) && this.isLeader;

        // ta ignorando até demais aqui

        System.out.println("[SERVER | " + this.id +"] shouldIgnoreMessage: (" +
            !imTheReceiver + " || " + subjectOnlyToLeader + " || " + subjectOnlyToSlave + ")");

        return !imTheReceiver || subjectOnlyToLeader || subjectOnlyToSlave;
    }

    private void handleJoinGroupRequestMessage(Message message) {
        boolean acceptNewMember = true;
        // TODO: valida o novo membro ...

        try {
            System.out.println("[SERVER | " + this.id +"] handleJoinGroupRequestMessage: server " +
                message.senderId + " was accepted (" + acceptNewMember + ")");

            String messageSubject = acceptNewMember ?
                Message.ACCEPT_NEW_MEMBER_SUBJECT :
                Message.REJECT_NEW_MEMBER_SUBJECT;

            this.remoteCoordinator.sendBroadcastMessage(
                new Message(this.id, messageSubject, message.senderId.toString())
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleClientRequestMessage(Message message) {
        System.out.println("[SERVER | " + this.id +"] handleClientRequestMessage: " +
            "handling message from client " + message.senderId);

        CommandExecutionThreadRunner nextCommandRunner = new CommandExecutionThreadRunner(message, () -> {
            // remover o comando que acabou de ser executado
            this.queueCommandsToExecute.poll();

            // pega o proximo para ser executado
            Thread nextCommand = this.queueCommandsToExecute.peek();

            System.out.println("[nextCommandRunnable] there is next command: " +
                (nextCommand == null ? "no" : "yes"));

            // se tiver algo para executar, executa
            if (nextCommand != null)
                nextCommand.start();
        });

        System.out.println("[SERVER | " + this.id +"] handleClientRequestMessage: " +
            "thread runner created: " + nextCommandRunner.getId());

        // insere a nova thread de execucao na fila
        this.queueCommandsToExecute.add(nextCommandRunner);

        System.out.println("[SERVER | " + this.id +"] handleClientRequestMessage: " +
            "thread runner added to queue");

        // caso seja a primeira, executa
        if (this.queueCommandsToExecute.getFirst().getId() == nextCommandRunner.getId()) {
            System.out.println("[SERVER | " + this.id +"] handleClientRequestMessage: " +
                "this runner is the first, its starting now");

            queueCommandsToExecute.getFirst().start();

            System.out.println("[SERVER | " + this.id +"] handleClientRequestMessage: " +
                "this runner was started");
        }

        //** o fluxo de adicionar na fila, mesmo que remova depois,
        //   pode ajudar a evitar casos de sincronia entre um add e
        //   o termino da execucao de outra thread, onde a proxima
        //   pode nao ser chamada
    }

    @Override
    public void setLeader(Boolean isLeader) {
        this.isLeader = isLeader;
        System.out.println("[SERVER | " + this.id + "] setLeader: i'm the new leader");
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {
        System.out.println("[Server|Main] trying to get registry");
        Registry registry = LocateRegistry.getRegistry(Constants.COORDINATOR_REGISTRY_PORT);

        System.out.println("[Server|Main] trying to get coordinator");
        IServerCoordinator serverCoordinator =
                (IServerCoordinator) registry.lookup(Constants.COORDINATOR_REGISTRY_NAME);

        Server server = new Server(serverCoordinator);
        System.out.println("[Server|Main] registering server " + server.getId());
        serverCoordinator.registerServer(server);
        System.out.println("[Server|Main] servers registered");

        while(!Thread.currentThread().isInterrupted());
    }
}
