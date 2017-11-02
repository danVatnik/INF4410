package shared;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RepartitorRegistering extends Remote {
	String REPARTITOR_NAME = "Repartitor";
	
	void bindSomething(String bindName, Remote objectToBind) throws AlreadyBoundException, RemoteException;
}
