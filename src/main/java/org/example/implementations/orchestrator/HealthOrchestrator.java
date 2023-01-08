package org.example.implementations.orchestrator;

import org.example.helpers.RemoteHelper;
import org.example.implementations.server.Server;
import org.example.interfaces.server.IDSServer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthOrchestrator {

    private static Map.Entry<String, IDSServer> getCurrentLeader(HashMap<String, IDSServer> servers) {
        for (Map.Entry<String, IDSServer> serverEntry: servers.entrySet()) {
            try {
                // caso tenha caido, o acesso direto vai dar erro
                if (serverEntry.getValue().getIsLeader())
                    return serverEntry;
            } catch (RemoteException e) {
                // de forma geral, nao eh lider
            }
        }

        return null;
    }

    /**
     * Verifica a saúde dos servidores lider e membros
     * Caso o servidor lider falhe, faz a eleiçao de um novo lider
     * Caso o servider membro falhe, o lider é invocado para remover o membro com falha
     * O membro falho deve ser removido da lista de servidores tambem
     */
    public static void checkServersHealth() throws RemoteException {
        HashMap<String, IDSServer> servers = RemoteHelper.getServers();
        System.out.println("[HealthOrchestrator] got " + servers.size() + " servers");

        if (servers.isEmpty())
            return;

        Map.Entry<String, IDSServer> currentLeader = getCurrentLeader(servers);
        System.out.println("[HealthOrchestrator] there's leader? " + (currentLeader != null));

        // se nao tiver lider ou se nao estiver 100%, inicia uma eleicao
        if (currentLeader == null || !currentLeader.getValue().imHealthy()) {
            ellectNewLeader(servers);
        }

        Map.Entry<String, IDSServer> finalCurrentLeader = getCurrentLeader(servers);
        servers.forEach((bindedName, server) -> {
            new Thread(() -> { // verificar em thread para nao travar a verificacao dos outros nos
                Boolean removeServer = false;

                try {
                    // se nao estiver bem e nao for lider, marca para remocao
                    if (!server.imHealthy() && !server.getIsLeader()) {
                        removeServer = true;
                    }
                } catch (RemoteException e) {
                    // caso tenha dado erro, pode ser que tenha caido,
                    // entao eh marcado para remocao
                    removeServer = true;
                }

                if (removeServer) {
                    try {
                        finalCurrentLeader.getValue().removeMember(bindedName);
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }).start();
        });

        System.out.println("[HealthOrchestrator] finished");
    }

    private static void ellectNewLeader(HashMap<String, IDSServer> servers) throws RemoteException {
        IDSServer newLeader = servers.values().stream()
            .filter(s -> {
                try {
                    return s.imHealthy();
                } catch (RemoteException e) {
                    return false;
                }
            })
            .findFirst()
            .orElse(null);

        if (newLeader == null)
            return;

        newLeader.setLeader(true);
        System.out.println("[HealthOrchestrator] new leader id " + newLeader.getId());
    }
}
