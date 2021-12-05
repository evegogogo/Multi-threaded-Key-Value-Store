package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public interface ServerInterface extends Remote {
    ConcurrentHashMap<String, String> getStorage() throws RemoteException;
    String getServerID() throws RemoteException;
    Response put(Request request) throws RemoteException;
    Response get(Request request) throws RemoteException;
    Response delete(Request request) throws RemoteException;
    Promise prepare(long proposalNum) throws RuntimeException, RemoteException;
    Accept accept(long proposalNum, Request request) throws RuntimeException, RemoteException;
    void invokeLearner(Accept accepted) throws RemoteException;
    void registerNewServer(String currentServerID, ServerInterface server) throws RemoteException;
}