package org.example.implementations.server;

import org.example.Main;
import org.example.constants.Constants;
import org.example.helpers.ServerHelper;
import org.example.implementations.commom.Message;
import org.example.implementations.commom.MessageSendingConfig;
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
import java.util.concurrent.ExecutionException;

public class Server extends UnicastRemoteObject implements IServer, IDSServer, Serializable {
    private Long id;
    private Boolean isLeader;
    private final ConcurrentLinkedDeque<Thread> queueCommandsToExecute;

    public Server() throws RemoteException, AlreadyBoundException, NotBoundException, ExecutionException, InterruptedException {
        this.queueCommandsToExecute = new ConcurrentLinkedDeque<>();
        this.isLeader = false;
        this.id = null;

        this.tryJoinGroup();
    }

    private void tryJoinGroup() throws RemoteException, AlreadyBoundException, NotBoundException, ExecutionException, InterruptedException {
        Registry registry = Main.getRegistry();

        // pegar a lista de servicos registrados
        List<String> registryBindedNames = ServerHelper.getServersRegistryBindedName();

        if (registryBindedNames.isEmpty()) {
            // obter o id do grupo
            this.id = ServerHelper.getNewServerId(registryBindedNames);

            // definir como lider
            this.setLeader(true);

            // cria o nome e registrar o servidor
            String serverBindName = Constants.SERVER_REGISTY_BOUND_BASE_NAME + this.id;
            registry.bind(serverBindName, this);

            System.out.println("[Server | " + this.id + "] server registered in '" + serverBindName + "'");

            return;
        }

        System.out.println("[Server | " + this.id + "] solicitando entrada no grupo");

        // mensagem solicitando entrada no grupo
        Message message = new Message(this.id, Message.REQUEST_JOIN_GROUP_SUBJECT);

        // configuracao de tratamento da msg
        MessageSendingConfig confg = new MessageSendingConfig()
            // funcao de contexto que retorna o callback para quando o servidor destinatario
            // retornar a resposta da mensagem
            .setWhenMessageComplete((resultCollector) -> {
                return (result, exeption) -> { // callback de resposta da mensagem

                    System.out.println("[Server | " + this.id + "] ack recebido do servidor " + result.getSenderId());

                    if (exeption != null) {
                        resultCollector.ackReceived(result, (responses) -> {
                            // callback caso essa resposta seja aultima em espera
                            // responsavel por reduzir as repostas em um valor que represente o conjunto
                            Optional<Message> optLeaderAnswer = responses.stream()
                                .filter(r ->
                                    !Objects.equals(r.getSubject(), Message.IGNORED_SUBJECT) && // nao ignorou (sendo um no)
                                    r.getMessage() != null // tendo resposta, eh do lider
                                ).findFirst();

                            Message finalResult = optLeaderAnswer.isEmpty() ?
                                new Message(null, Message.UNANSWERED_SUBJECT) :
                                optLeaderAnswer.get();

                            System.out.println("[Server | " + this.id + "] todos os acks foram recebidos " + finalResult.getSubject());

                            return finalResult;
                        });
                    }
                };
            });

        System.out.println("[Server | " + this.id + "] mensagem e config de envio criadas");

        // envio da mensagem
        ResultsCollector<Message> collector = ServerHelper.sendBroadcastMessage(message, confg);

        System.out.println("[Server | " + this.id + "] mensagem enviada via broadcast");

        Message joinResult = collector.get();

        System.out.println("[Server | " + this.id + "] result obtido: " + joinResult.getSubject());

        if (joinResult.getSubject().equals(Message.ACCEPT_NEW_MEMBER_SUBJECT)) {
            // obter o id do grupo
            this.id = ServerHelper.getNewServerId();

            // cria o nome e registrar o servidor
            String serverBindName = Constants.SERVER_REGISTY_BOUND_BASE_NAME + this.id;
            registry.bind(serverBindName, this);

            System.out.println("[Server | " + this.id + "] server registered in '" + serverBindName + "'");
        }
    }

    @Override
    public long getId() { return this.id; }

    @Override
    public void receiveMessage(Message message) {
        System.out.println("[SERVER | " + this.id +"] receiveMessage: " + message.getSubject() + " -- " + message.getMessage());

        Boolean shouldIgnoreMessage = this.shouldIgnoreMessage(message);
        System.out.println("[SERVER | " + this.id +"] receiveMessage: shouldIgnoreMessage: " + shouldIgnoreMessage);

        if (shouldIgnoreMessage) {
            message.reply(new Message(this.getId(), Message.IGNORED_SUBJECT));
            return;
        }

        switch (message.getSubject()) {
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
        // lista de assuntos tratados apenas pelo lider
        List<String> leaderSubjects = Arrays.asList(
            Message.CLIENT_REQUEST_SUBJECT,
            Message.REQUEST_JOIN_GROUP_SUBJECT
        );

        // lista de assuntos tratados apenas pelos escravos
        List<String> slaveSubjects = List.of(
            Message.REPLICATE_REQUEST_SUBJECT
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

            message.reply(new Message(this.getId(), messageSubject, Boolean.toString(acceptNewMember)));

            System.out.println("[SERVER | " + this.id +"] handleJoinGroupRequestMessage: server " +
                    message.getSenderId() + " was accepted (" + acceptNewMember + ")");

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

        System.out.println("[SERVER | " + this.id + "] " +
            "setLeader: i'm the new leader? " + isLeader);
    }

    public static void main(String[] args) throws RemoteException, AlreadyBoundException, NotBoundException, ExecutionException, InterruptedException {
        Server server = new Server();
    }
}
