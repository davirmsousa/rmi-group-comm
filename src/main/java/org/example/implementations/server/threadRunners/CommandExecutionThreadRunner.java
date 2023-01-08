package org.example.implementations.server.threadRunners;

import org.example.helpers.RemoteHelper;
import org.example.implementations.commom.Message;
import org.example.implementations.commom.ResultsCollector;

import java.util.Objects;

public class CommandExecutionThreadRunner extends Thread {
    private final Message message;
    private final Runnable nextCommandRunnable;
    private ResultsCollector<Message> collector;

    public CommandExecutionThreadRunner(Message message, Runnable nextCommandRunnable) {
        this.nextCommandRunnable = nextCommandRunnable;
        this.message = message;
    }

    @Override
    public void run() {
        System.out.println("[commandExecutionThread | " + this.getId() +"] commandExecution starting now");

        try {
            // abrir uma transacao
            // TODO: usar repositorio para iniciar a transacao
            System.out.println("[commandExecutionThread | " + this.getId() +"] begin transaction");

            // executar a consulta
            // TODO: usar repositorio para rodar o comando
            System.out.println("[commandExecutionThread | " + this.getId() +"] executed command: " + this.message.getMessage());

            Message prepareMessage = new Message(message.getRecipientId(), Message.NODE_PREPARE_REQUEST_SUBJECT);

            collector = new ResultsCollector<>(null, (responses) -> {
                Boolean allPrepared = responses.stream().allMatch(r ->
                    Objects.equals(r.getSubject(), Message.NODE_PREPARE_RESPONSE_SUBJECT) &&
                    Boolean.parseBoolean(r.getMessage())
                );

                Message reducedMessage = new Message(null, Message.NODE_PREPARE_RESPONSE_SUBJECT);
                reducedMessage.setMessage(allPrepared.toString());

                return reducedMessage;
            });

            // mandar a mensagem pros nos
            RemoteHelper.sendMessage(prepareMessage, collector);

            // esperar que todos tenham respondido
            while(!collector.isDone()) { Thread.sleep(500); }

            /*
            TODO: preciso encontrar um jeito de fazer a thread principal (servidor lider), a que recebe a mensagem, e essa thread se comunicarem
             pois quando eu mandar a mensagem de 'prepare' e 'commit' preciso esperar por todas as respostas:
                - mando a mensagem em broadcast para os slaves com o id desta thread
                - eles respondem a mensagem tambem com o id da thread
                    - assim posso ir na lista, buscar a thread e gerar uma notificacao
                - esta thread principal (runner) precisa esperar que todos respondam ou ate o tempo de timeout estourar
                    - toda mensagem tem que ter um id, e vou sempre esperar uma resposta da mensagem com esse id
                        assim, se o slave responser atrasado, nao vou contar uma mensagem errada
            */

            // fazer o commit
            // TODO: usar repositorio para fazer o commit
            System.out.println("[commandExecutionThread | " + this.getId() +"] commited");
        } catch (Exception ignored) {
            // fazer o rollback
            // TODO: usar repositorio para fazer o rollback
            System.out.println("[commandExecutionThread | " + this.getId() +"] something is wrong, " +
                "transaction rolled back. " + message.getMessage());
        }

        System.out.println("[commandExecutionThread | " + this.getId() +"] calling nextCommandRunnable");

        // executando proximo comando da fila
        this.nextCommandRunnable.run();

        System.out.println("[commandExecutionThread | " + this.getId() +"] commandExecution is ending now");
        // a thread da execucao atual morre
    }
}
