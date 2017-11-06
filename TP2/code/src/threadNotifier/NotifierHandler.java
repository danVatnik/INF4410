package threadNotifier;

import java.util.HashMap;
import java.util.LinkedList;

public class NotifierHandler<T extends IThreadNotifier> {

	private final HashMap<T, T[]> pairedThreads = new HashMap<>();
	private final LinkedList<IThreadNotifier> threadsFinished = new LinkedList<>();
	
	public void startNewThreads(T[] threadsToStart) {
		for(T thread : threadsToStart) {
			pairedThreads.put(thread, threadsToStart);
			thread.setFinishedCollection(threadsFinished);
			thread.start();
		}
	}
	
	public T[] getFinishedThreadsPool() throws InterruptedException {
		T[] poolFinished = null;
		while (poolFinished == null) {
			synchronized (threadsFinished) {
				if(threadsFinished.size() > 0) {
					poolFinished = threatFinishedThreads();
				}
				else {
					threadsFinished.wait();
					poolFinished = threatFinishedThreads();
				}
			}
		}
		return poolFinished;
	}
	
	public boolean haveUngetResults() {
		return pairedThreads.size() > 0;
	}
	
	private T[] threatFinishedThreads() throws InterruptedException {
		T[] poolFinished = null;
		while(poolFinished == null && threadsFinished.size() > 0) {
			IThreadNotifier threadFinished = threadsFinished.peekFirst();
			threadFinished.join();
			threadsFinished.removeFirst();
			poolFinished = pairedThreads.get(threadFinished);
			int i = 0;
			while(poolFinished != null && i < poolFinished.length) {
				if(poolFinished[i].getState() != Thread.State.TERMINATED) {
					poolFinished = null;
				}
				++i;
			}
			if(poolFinished != null) {
				for(T thread : poolFinished) {
					pairedThreads.remove(thread);
				}
			}
		}
		return poolFinished;
	}
}
