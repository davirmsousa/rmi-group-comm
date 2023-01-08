package org.example.implementations.server.threadRunners;

import org.example.implementations.commom.Message;

public class CommandExecutionThreadRunner extends Thread {
    private final Message message;
    private final Runnable nextCommandRunnable;

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

            // executar a consulta
            // TODO: usar repositorio para rodar o comando

            // iniciar o 3P-Commit
            System.out.println("[commandExecutionThread | " + this.getId() +"] executed command: " + this.message.getMessage());

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
        } catch (Exception ignored) {
            // fazer o rollback
            // TODO: usar repositorio para fazer o rollback
        }

        System.out.println("[commandExecutionThread | " + this.getId() +"] calling nextCommandRunnable");

        // executando proximo comando da fila
        this.nextCommandRunnable.run();

        System.out.println("[commandExecutionThread | " + this.getId() +"] commandExecution is ending now");
        // a thread da execucao atual morre
    }
}
