package org.example.implementations.server;

import org.example.Main;
import org.example.constants.Constants;
import org.example.helpers.RemoteHelper;
import org.example.implementations.commom.Message;
import org.example.implementations.commom.ResultsCollector;
import org.example.implementations.server.threadRunners.CommandExecutionThreadRunner;
import org.example.interfaces.server.IDSServer;
import org.example.interfaces.server.IServer;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public class Server extends UnicastRemoteObject implements IServer, IDSServer, Serializable {
    private Long id;
    private Boolean isLeader;
    private final ConcurrentLinkedDeque<Thread> queueCommandsToExecute;
    private final ConcurrentLinkedQueue<ResultsCollector<Message>> messagesWaitingResponse;

    public Server() throws RemoteException, AlreadyBoundException, NotBoundException, ExecutionException, InterruptedException {
        this.messagesWaitingResponse = new ConcurrentLinkedQueue<>();
        this.queueCommandsToExecute = new ConcurrentLinkedDeque<>();
        this.isLeader = false;
        this.id = null;

        this.tryJoinGroup();
    }

    private void tryJoinGroup() throws RemoteException, AlreadyBoundException, NotBoundException {
        Registry registry = Main.getRegistry();

        // pegar a lista de servicos registrados
        List<String> registryBindedNames = RemoteHelper.getServersRegistryBindedName(registry);

        if (registryBindedNames.isEmpty()) {
            // definir como lider
            this.setLeader(true);
            // obter o id do servidor na lista
            this.id = RemoteHelper.getNewItemId(registryBindedNames);
            // cria o nome e registrar o servidor
            String serverBindName = Constants.SERVER_REGISTY_BOUND_BASE_NAME + this.id;
            registry.bind(serverBindName, this);
            System.out.println("[Server | " + this.id + "] server registered in '" + serverBindName + "'");

            return;
        }

        // registrar como pendente
        List<String> pendingServers = RemoteHelper.getPendingServersRegistryBindedName(registry);
        // obter o id do servidor na lista
        this.id = RemoteHelper.getNewItemId(pendingServers);
        // cria o nome e registrar o servidor
        String pServerBindName = Constants.WAITING_SERVER_REGISTY_BOUND_BASE_NAME + this.id;
        registry.bind(pServerBindName, this);
        System.out.println("[Server | " + this.id + "] server registered in '" + pServerBindName + "'");

        System.out.println("[Server | " + this.id + "] solicitando entrada no grupo");
        // mensagem solicitando entrada no grupo
        Message message = new Message(this.id, Message.REQUEST_JOIN_GROUP_SUBJECT);
        message.setFromRoute(Constants.WAITING_SERVER_REGISTY_BOUND_BASE_NAME);

        ResultsCollector<Message> collector = new ResultsCollector<>((that, response) -> {
            if (Objects.equals(response.getSubject(), Message.ACCEPT_NEW_MEMBER_SUBJECT)) {
                that.complete(response);
                System.out.println("[SERVER | " + this.id +"] coletor foi completo e o servidor foi aceito");
                this.messagesWaitingResponse.removeIf(c -> c.getOriginalMessageId() == message.getId());
                System.out.println("[SERVER | " + this.id +"] coletor removido da lista de espera");
            } else if (Objects.equals(response.getSubject(), Message.REJECT_NEW_MEMBER_SUBJECT)) {
                that.complete(response);
                System.out.println("[SERVER | " + this.id +"] coletor foi completo e o servidor foi rejeitado");
                this.messagesWaitingResponse.removeIf(c -> c.getOriginalMessageId() == message.getId());
                System.out.println("[SERVER | " + this.id +"] coletor removido da lista de espera");
            }

            return response;
        }, null);
        collector.setOriginalMessageId(message.getId());

        this.messagesWaitingResponse.add(collector);
        System.out.println("[SERVER | " + this.id +"] coletor adicionado na lista de espera");

        // envio da mensagem
        RemoteHelper.sendMessage(message, collector);
        System.out.println("[SERVER | " + this.id +"] mensagens enviadas");
    }

    @Override
    public long getId() { return this.id; }

    @Override
    public void receiveMessage(Message message) {
        System.out.println("[SERVER | " + this.id +"] receiveMessage: " + message.getSubject() + " -- " + message.getMessage());

        Boolean shouldIgnoreMessage = this.shouldIgnoreMessage(message);
        System.out.println("[SERVER | " + this.id +"] receiveMessage: shouldIgnoreMessage: " + shouldIgnoreMessage);

        if (shouldIgnoreMessage) {
            return;
        }

        switch (message.getSubject()) {
            case Message.REQUEST_JOIN_GROUP_SUBJECT:
                this.handleJoinGroupRequestMessage(message);
                break;
            case Message.CLIENT_REQUEST_SUBJECT:
                this.handleClientRequestMessage(message);
                break;
            case Message.ACCEPT_NEW_MEMBER_SUBJECT:
            case Message.REJECT_NEW_MEMBER_SUBJECT:
                this.handleNewMemberResponse(message);
                break;
            case Message.NODE_PREPARE_RESPONSE_SUBJECT:
                this.handleNewMemberResponse(message);
                break;
            default:
                break;
        }
    }

    private Boolean shouldIgnoreMessage(Message message) {
        // lista de assuntos tratados apenas pelo lider
        List<String> leaderSubjects = Arrays.asList(
            Message.CLIENT_REQUEST_SUBJECT,
            Message.REQUEST_JOIN_GROUP_SUBJECT,
            Message.NODE_PREPARE_RESPONSE_SUBJECT
        );

        // lista de assuntos tratados apenas pelos escravos
        List<String> slaveSubjects = Arrays.asList(
            Message.NODE_COMMIT_REQUEST_SUBJECT,
            Message.NODE_PREPARE_REQUEST_SUBJECT
        );

        boolean subjectOnlyToLeader = leaderSubjects.contains(message.getSubject()) && !this.isLeader;
        boolean subjectOnlyToSlave = slaveSubjects.contains(message.getSubject()) && this.isLeader;

        System.out.println("[SERVER | " + this.id +"] shouldIgnoreMessage: (" +
                subjectOnlyToLeader + " || " + subjectOnlyToSlave + ")");

        return subjectOnlyToLeader || subjectOnlyToSlave;
    }

    private synchronized void handleJoinGroupRequestMessage(Message message) {
        boolean acceptNewMember = true;

        // TODO: valida o novo membro ...

        try {

            String messageSubject = acceptNewMember ?
                    Message.ACCEPT_NEW_MEMBER_SUBJECT :
                    Message.REJECT_NEW_MEMBER_SUBJECT;

            System.out.println("[SERVER | " + this.id +"] handleJoinGroupRequestMessage: server " +
                    message.getSenderId() + " was accepted (" + acceptNewMember + ")");

            if (acceptNewMember) {
                // remover da lista pendente
                Registry registry = Main.getRegistry();
                String serverUnbindName = Constants.WAITING_SERVER_REGISTY_BOUND_BASE_NAME + message.getSenderId();
                IDSServer newServer = (IDSServer) registry.lookup(serverUnbindName);
                registry.unbind(serverUnbindName);
                System.out.println("[Server | " + this.id + "] new member removed from '" + serverUnbindName + "'");

                // adicionar na lista de servidores
                newServer.setId(RemoteHelper.getNewRegisteredServerId());
                String serverBindName = Constants.SERVER_REGISTY_BOUND_BASE_NAME + newServer.getId();
                registry.bind(serverBindName, newServer);
                System.out.println("[Server | " + this.id + "] server registered in '" + serverBindName + "'");
            }

            // criar a mensagem de resposta
            Message answer = new Message(this.getId(), messageSubject);
            answer.setMessage(Boolean.toString(acceptNewMember));
            answer.setAnsweredMessage(message);

            // manda a resposta sem esperar por uma resposta
            RemoteHelper.sendMessage(answer, new ResultsCollector<>());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleClientRequestMessage(Message message) {
        System.out.println("[SERVER | " + this.id +"] handleClientRequestMessage: " +
            "handling message from client " + message.getSenderId());

        CommandExecutionThreadRunner nextCommandRunner = new CommandExecutionThreadRunner(message, () -> {
            // remover o comando que acabou de ser executado
            this.queueCommandsToExecute.poll();

            // pega o proximo para ser executado
            Thread nextCommand = this.queueCommandsToExecute.peek();

            System.out.println("[nextCommandRunnable] there is next command: " +
                (nextCommand == null ? "no" : "yes"));

            Message answer = new Message(this.id, Message.SERVER_RESPONSE_SUBJECT);
            answer.setRoute(Constants.CLIENT_REGISTY_BOUND_BASE_NAME);
            answer.setRecipientId(message.getSenderId());
            answer.setAnsweredMessage(message);
            answer.setMessage("nao sei como terminou");

            try { RemoteHelper.sendMessage(answer, new ResultsCollector<>()); }
            catch (RemoteException | NotBoundException ignored) {  }

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

    private void handleNewMemberResponse(Message message) {
        try {
            // pegar o coletor dessa mensagem
            Optional<ResultsCollector<Message>> optCollector = messagesWaitingResponse.stream()
                    .filter(c -> c.getOriginalMessageId().equals(message.getAnsweredMessage().getId()))
                    .findFirst();

            System.out.println("[SERVER | " + this.id +"] servidor em espera por resposta? " +
                optCollector.isPresent());

            // tartar se conseguiu achar o coletor
            if (optCollector.isEmpty())
                return;

            // notificar que recebeu uma resposta
            optCollector.get().ackReceived(message);

            System.out.println("[SERVER | " + this.id +"] coletor notificado");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setLeader(Boolean isLeader) {
        this.isLeader = isLeader;
    }

    @Override
    public Boolean getIsLeader() throws RemoteException {
        return this.isLeader;
    }

    @Override
    public void setId(long id) throws RemoteException {
        this.id = id;
        System.out.println("[SERVER | " + this.id +"] im the new leader");
    }

    @Override
    public Boolean imHealthy() throws RemoteException {
        // TODO: validar conexao com o banco ou outras coisas
        return true;
    }

    @Override
    public void removeMember(String unhealthyServerBindedName) throws RemoteException {
        if (!isLeader) {
            System.out.println("[SERVER | " + this.id +"] " +
                "cant remove server, im not the leader");
            return;
        }

        try {
            Registry registry = Main.getRegistry();
            registry.unbind(unhealthyServerBindedName);

            System.out.println("[SERVER | " + this.id +"] " +
                "servidor removido: " + unhealthyServerBindedName);
        } catch (Exception ignore) {
            System.out.println("[SERVER | " + this.id +"] " +
                "error ao remover server " + unhealthyServerBindedName);
        }
    }

    public static void main(String[] args) throws RemoteException, AlreadyBoundException, NotBoundException, ExecutionException, InterruptedException {
        Server server = new Server();
    }
}
