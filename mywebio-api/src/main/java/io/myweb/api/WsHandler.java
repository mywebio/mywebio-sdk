package io.myweb.api;

public interface WsHandler {

	void onConnected(WsConnection conn);

	void onDisconnected(WsConnection conn);

	void onMessage(WsConnection conn, Object msg);
}
