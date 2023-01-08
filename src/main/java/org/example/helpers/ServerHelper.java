package org.example.helpers;

import org.example.Main;
import org.example.constants.Constants;
import org.example.implementations.commom.Message;
import org.example.implementations.commom.MessageSendingConfig;
import org.example.implementations.commom.ResultsCollector;
import org.example.implementations.commom.SCompletableFuture;
import org.example.interfaces.server.IDSServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerHelper {

    public static long getNewServerId() throws RemoteException {
        return getNewServerId(getServersRegistryBindedName());
    }

    public static long getNewServerId(List<String> bindedServers) {
        if (bindedServers.isEmpty())
            return 0;

        String lastBindedServer = bindedServers.get(bindedServers.size() - 1);
        String lastServerId = lastBindedServer.split("/")[1];
        return Long.parseLong(lastServerId) + 1;
    }

    public static List<String> getServersRegistryBindedName() throws RemoteException {
        return getServersRegistryBindedName(Main.getRegistry());
    }

    public static List<String> getServersRegistryBindedName(Registry registry) throws RemoteException {
        // pegar a lista de nomes vinculados
        List<String> registryBindedNames = Arrays.asList(registry.list());

        return registryBindedNames.stream()
                // filtrar apenas pelos servidores
                .filter(bindedName -> bindedName.startsWith(Constants.SERVER_REGISTY_BOUND_BASE_NAME))
                // transformar em lista
                .collect(Collectors.toList());
    }

    public static List<IDSServer> getServers() {
        try {
            Registry registry = Main.getRegistry();
            List<String> bindedServers = getServersRegistryBindedName(registry);

            List<IDSServer> servers = new ArrayList<>();

            for (String bindServerName : bindedServers) {
                IDSServer server = (IDSServer) registry.lookup(bindServerName);
                servers.add(server);
            }

            return servers;
        } catch (RemoteException | NotBoundException ignored) { }

        return new ArrayList<>();
    }

    public static ResultsCollector<Message> sendBroadcastMessage(Message message, MessageSendingConfig config)
            throws RemoteException, NotBoundException {
        Registry registry = Main.getRegistry();

        // pegar o nome dos servidores e remover o emissor da lista
        List<String> serversBindedNames = getServersRegistryBindedName(registry)
                .stream().filter(sbn ->
                    message.getSenderId() == null || // mensagem de um nao-membro
                    !sbn.endsWith(message.getSenderId().toString()) // servidor que nao seja o remetente
                )
                .collect(Collectors.toList());

        System.out.println("[Server] enviando mensagem " + message.getSubject() +
                " para " + serversBindedNames.size() + " servidores");

        // criar o coletor de resultados
        ResultsCollector<Message> collector = new ResultsCollector();

        // define o callback de completude de todas as mensagens
        var whenCollectorComplete = config.getWhenCollectorComplete();
        if (whenCollectorComplete != null)
            collector.whenComplete(whenCollectorComplete);

        for (String serverBindedName : serversBindedNames) {
            // pegar o objeto remoto do servidor
            IDSServer recipientServer = (IDSServer) registry.lookup(serverBindedName);

            // clonar a mensagem
            Message cloneMessage = message.clone();

            SCompletableFuture<Message> futureMessage = new SCompletableFuture<>();

            // define o callback de resposta da mensagem
            var whenMessageComplete = config.getWhenMessageComplete(collector);
            if (whenMessageComplete != null)
                futureMessage.whenComplete(whenMessageComplete);

            if (config.getTimeout() != null) {
                // define o timeout de acordo com a configuracao
                futureMessage.orTimeout(config.getTimeout(), config.getTimeoutUnit());
            }

            // define o canal de resposta da mensagem
            cloneMessage.setResponseChannel(futureMessage);

            System.out.println("[Server] enviando mensagem para destinatario " + recipientServer.getId());

            // mandar a mensagem para o servidor
            recipientServer.receiveMessage(cloneMessage);

            collector.messageSent(cloneMessage);
        }

        return collector;
    }
}
