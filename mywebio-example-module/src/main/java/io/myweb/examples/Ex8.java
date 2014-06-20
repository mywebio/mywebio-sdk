package io.myweb.examples;

import io.myweb.api.WebSocket;
import io.myweb.api.WsConnection;
import io.myweb.api.WsHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Ex8 {

	@WebSocket("/ws")
	public WsHandler webSocket() {
		return new WsHandler() {

			private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

			@Override
			public void onConnected(final WsConnection conn) {
				executor.schedule(new Runnable() {
					@Override
					public void run() {
						conn.push(System.currentTimeMillis());
					}
				}, 1, TimeUnit.SECONDS);
			}

			@Override
			public void onDisconnected(WsConnection conn) {
				executor.shutdown();
			}

			@Override
			public void onMessage(WsConnection conn, Object msg) {
				conn.push(msg);
			}
		};
	}


	/*
	 JavaScript code:

	 var ws = new WebSocket("ws://localhost/eu.javart.androidwebmodule/ws");
	 ws.onmessage = function(evt) {
         console.log("timeInMillis: " + evt.data);
     };

	*/
}
