package io.myweb.test;


import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import io.myweb.Service;
import io.myweb.test.support.MywebTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;


public class WowFirstWorkingTest extends MywebTestCase {

	public static final String GET_1 =
			"GET /test HTTP/1.1\r\n" +
			"Host: localhost\r\n" +
			"Connection: close\r\n\r\n";

	public static final String POST_BODY = "id=11&name=somename";
	public static final String POST_1 =
			"POST /testpost HTTP/1.1\r\n" +
			"Host: localhost\r\n" +
			"Content-length: "+POST_BODY.length()+"\r\n\r\n" +
			POST_BODY;

	public static final String GET_LONG_1 =
			"GET /test HTTP/1.1\r\n" +
			"Cookie: cookieName=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+
			"\r\n" +
			"Host: localhost\r\n" +
			"Connection: Keep-Alive\r\n\r\n";

	@Test(timeout = 1000)
	public void simplestGet() throws IOException, InterruptedException {
		// given
		Service service = startService();

		// when
		LocalSocket clientSocket = new LocalSocket();
		clientSocket.connect(new LocalSocketAddress("doesn't matter now"));
		OutputStream os = clientSocket.getOutputStream();
		os.write(GET_1.getBytes());
		os.close();

		// then
		InputStream is = clientSocket.getInputStream();
		String response = IOUtils.toString(is);
		System.out.println("response: " + response);
		String expectedBody = new Test1().test();
		assertThat(response, containsString(expectedBody));
	}

	@Test(timeout = 1000)
	public void simplestPost() throws IOException, InterruptedException {
		// given
		Service service = startService();

		// when
		LocalSocket clientSocket = new LocalSocket();
		clientSocket.connect(new LocalSocketAddress("doesn't matter now"));
		OutputStream os = clientSocket.getOutputStream();
		os.write(POST_1.getBytes());
		os.close();

		// then
		InputStream is = clientSocket.getInputStream();
		String response = IOUtils.toString(is);
		System.out.println("response: " + response);
		String expectedBody = new TestPost().post(11, "somename");
		assertThat(response, containsString(expectedBody));
	}

	private Service startService() {
		Service service = new Service();
		service.onCreate();
		service.onStartCommand(new Intent(), 0, 0);
		return service;
	}

	@Test(timeout = 1000)
	public void shouldParseLongGetRequests() throws IOException {
		// given
		Service service = startService();

		// when
		LocalSocket clientSocket = new LocalSocket();
		clientSocket.connect(new LocalSocketAddress("doesn't matter now"));
		OutputStream os = clientSocket.getOutputStream();
		os.write(GET_LONG_1.getBytes()); // two long requests
		os.write(GET_LONG_1.getBytes());
		os.close();

		// then
		InputStream is = clientSocket.getInputStream();
		String response = IOUtils.toString(is);
		System.out.println("response: " + response);
		String expectedBody = new Test1().test();
		assertThat(response, containsString(expectedBody));
	}
}
