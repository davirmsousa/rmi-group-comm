package org.example.interfaces.server;

import org.example.interfaces.distributed.IDSObject;

import java.rmi.RemoteException;

public interface IDSServer extends IDSObject {
    void setLeader(Boolean isLeader) throws RemoteException;
}
