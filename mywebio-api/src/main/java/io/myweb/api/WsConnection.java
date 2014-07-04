package io.myweb.api;

public interface WsConnection {

	void push(Object msg);

	void disconnect();
}
