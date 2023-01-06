package org.example.interfaces.server;

import org.example.interfaces.distributed.IDSObject;

public interface IDSServer extends IDSObject {
    void setLeader(Boolean isLeader);
}
