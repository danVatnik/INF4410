package shared;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RepartitorRegistering extends Remote {
	void bindSomething(String bindName, Remote objectToBind) throws AlreadyBoundException, RemoteException;
}
