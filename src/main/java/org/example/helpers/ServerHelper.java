package org.example.helpers;

import org.example.Main;
import org.example.constants.Constants;
import org.example.implementations.commom.Message;
import org.example.implementations.commom.ResultsCollector;
import org.example.interfaces.server.IDSServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServerHelper {

    public static long getNewServerId() throws RemoteException {
        return getNewServerId(getServersRegistryBindedName());
    }

    public static long getNewServerId(List<String> bindedServers) {
        if (bindedServers.isEmpty())
            return 0;

        String lastBindedServer = bindedServers.get(bindedServers.size() - 1);
        String[] splitResult = lastBindedServer.split("/");
        String lastServerId = splitResult[splitResult.length - 1];
        return Long.parseLong(lastServerId) + 1;
    }

    public static List<String> getPendingServersRegistryBindedName() throws RemoteException {
        return getPendingServersRegistryBindedName(Main.getRegistry());
    }

    public static List<String> getPendingServersRegistryBindedName(Registry registry) throws RemoteException {
        return getRegistryBindedNameByKey(registry, Constants.WAITING_SERVER_REGISTY_BOUND_BASE_NAME);
    }

    public static List<String> getServersRegistryBindedName() throws RemoteException {
        return getServersRegistryBindedName(Main.getRegistry());
    }

    public static List<String> getServersRegistryBindedName(Registry registry) throws RemoteException {
        return getRegistryBindedNameByKey(registry, Constants.SERVER_REGISTY_BOUND_BASE_NAME);
    }

    public static List<String> getRegistryBindedNameByKey(Registry registry, String key) throws RemoteException {
        // pegar a lista de nomes vinculados
        List<String> registryBindedNames = Arrays.asList(registry.list());

        return registryBindedNames.stream()
                // filtrar apenas pelos servidores
                .filter(bindedName -> bindedName.startsWith(key))
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

    public static void sendMessage(Message message, ResultsCollector<Message> collector)
            throws RemoteException, NotBoundException {
        Registry registry = Main.getRegistry();

        // pegar o nome dos servidores e remover o emissor da lista
        List<String> serversBindedNames = Objects.equals(message.getRoute(), Constants.WAITING_SERVER_REGISTY_BOUND_BASE_NAME) ?
                getPendingServersRegistryBindedName(registry) :
                getServersRegistryBindedName(registry);

        serversBindedNames = serversBindedNames.stream().filter(sbn ->
                message.getSenderId() == null || // mensagem de um nao-membro
                (!sbn.startsWith(message.getFromRoute()) || !sbn.endsWith(message.getSenderId().toString())) && // servidor que nao seja o remetente
                (message.getRecipientId() == null || sbn.endsWith(message.getRecipientId().toString())) // se informar um destinatario
            )
            .collect(Collectors.toList());

        System.out.println("[Server] enviando mensagem " + message.getSubject() +
                " para " + serversBindedNames.size() + " servidores");

        collector.setExpectedAmountMessagensToSend(serversBindedNames.size());
        collector.setOriginalMessageId(message.getId());

        for (String serverBindedName : serversBindedNames) {
            // pegar o objeto remoto do servidor
            IDSServer recipientServer = (IDSServer) registry.lookup(serverBindedName);

            System.out.println("[Server] enviando mensagem para destinatario " + recipientServer.getId());

            // clonar a mensagem para setar o recipient
            Message cloneMessage = message.clone();
            cloneMessage.setRecipientId(recipientServer.getId());

            // mandar a mensagem para o servidor
            new Thread(() -> {
                try { recipientServer.receiveMessage(cloneMessage); }
                catch (RemoteException ignored) { }
            }).start();

            // registrar o envio no coletor
            collector.messageSent(cloneMessage);
        }
    }
}
