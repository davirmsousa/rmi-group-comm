package org.example.interfaces.server;

import org.example.interfaces.distributed.IDSObject;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface IDSServer extends IDSObject {
    void setLeader(Boolean isLeader) throws RemoteException;
    Boolean getIsLeader() throws RemoteException;
    void setId(long id) throws RemoteException;
    Boolean imHealthy() throws RemoteException;
    void removeMember(String unhealthyServerBindedName) throws RemoteException;
}
