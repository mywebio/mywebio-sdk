package io.myweb;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactories {

	public static ThreadFactory newServerThreadFactory() {
		return new ThreadFactory() {
			public Thread newThread(Runnable r) {
				return new Thread(r, "MywebioServerThread");
			}
		};
	}

	public static ThreadFactory newWorkerThreadFactory() {
		return new ThreadFactory() {
			private AtomicInteger id = new AtomicInteger(0);
			public Thread newThread(Runnable r) {
				return new Thread(r, "MywebioWorkerThread-" + id.getAndIncrement());
			}
		};
	}
}
