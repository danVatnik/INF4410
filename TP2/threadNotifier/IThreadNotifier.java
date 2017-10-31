package threadNotifier;

import java.util.Collection;

public interface IThreadNotifier {
	
	void setFinishedCollection(Collection<IThreadNotifier> finishedThreads);
	Thread.State getState();
	void join() throws InterruptedException;
	void start();
}
