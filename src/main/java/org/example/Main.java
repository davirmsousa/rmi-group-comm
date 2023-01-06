package org.example;

import org.example.constants.Constants;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
        registerServers();
    }

    private static void registerServers() {
        try {
            Registry registry = LocateRegistry.getRegistry(Constants.COORDINATOR_REGISTRY_PORT);

            java.rmi.Remote obj = registry.lookup(Constants.COORDINATOR_REGISTRY_NAME);
            System.out.println(obj.getClass().getCanonicalName());
            for (java.lang.reflect.AnnotatedType type: obj.getClass().getAnnotatedInterfaces()) {
                System.out.println(type.getType());
            }
        } catch (NotBoundException | RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}