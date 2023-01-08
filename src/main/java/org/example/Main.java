package org.example;

import org.example.constants.Constants;
import org.example.implementations.orchestrator.HealthOrchestrator;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    private static Registry registry;
    private static Thread healthOrchestratorThread;

    public static void main(String[] args) throws RemoteException {
        // cria o registry
        getRegistry();

        // roda o healthcheck
        healthOrchestratorThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        HealthOrchestrator.checkServersHealth();
                        Thread.sleep(3000);
                    } catch (RemoteException | InterruptedException ignore) { }
                }
            }
        };
        healthOrchestratorThread.start();

        System.out.println("[Main] running");
        // mantem a thread viva
        while(!Thread.currentThread().isInterrupted());
    }

    /**
     Obtem o registry padrao da aplicacao, criando-o caso nao exista.

     o registry precisa ficar numa variavel statica para evitar que o
     GarbageCollector remova-o da memoria, e precisa fica fora do contexto
     do Servidor pois (1) nao vai ser apenas o servidor quem vai usar e
     (2) quando o servidor morre o registry morre tambem, assim os demais processos
     (cliente, outros servidores, orquestrador) nao iriam conseguir pegar o mesmo registry novamente.
     */
    public static Registry getRegistry() throws RemoteException {
        // obtem o registry
        registry = LocateRegistry.getRegistry(Constants.DEFAULT_PORT);

        try {
            // testar se o registry existe
            registry.list();
            System.out.println("[Main] got existing registry");
        } catch (RemoteException e) {
            // nao existindo, cria
            registry = LocateRegistry.createRegistry(Constants.DEFAULT_PORT);
            System.out.println("[Main] new registry created");
        }

        return registry;
    }
}