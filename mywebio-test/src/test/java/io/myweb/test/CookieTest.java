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

public class CookieTest extends MywebTestCase {

	public static final String GET_1 =
			"GET /setCookie?name=COOKIE_NAME&value=cookieValue HTTP/1.1\r\n" +
			"Host: localhost\r\n" +
			"Connection: Keep-Alive\r\n\r\n";

	public static final String GET_2 =
			"GET /acceptCookie?cookieName=COOKIE_NAME HTTP/1.1\r\n" +
			"Host: localhost\r\n" +
			"Cookie: COOKIE_NAME=cookieValue\r\n" +
			"Connection: Keep-Alive\r\n\r\n";

	@Test(timeout = 1000)
	public void cookieShouldBeReturned() throws IOException, InterruptedException {
		// given
		Service service = new Service();
		service.onCreate();
		service.onStartCommand(new Intent(), 0, 0);

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
		String expectedBody = "Set-Cookie: COOKIE_NAME=cookieValuecookieValue";
		assertThat(response, containsString(expectedBody));
	}

	@Test(timeout = 1000)
	public void cookieShouldBeAccepted() throws IOException, InterruptedException {
		// given
		Service service = new Service();
		service.onCreate();
		service.onStartCommand(new Intent(), 0, 0);

		// when
		LocalSocket clientSocket = new LocalSocket();
		clientSocket.connect(new LocalSocketAddress("doesn't matter now"));
		OutputStream os = clientSocket.getOutputStream();
		os.write(GET_2.getBytes());
		os.close();

		// then
		InputStream is = clientSocket.getInputStream();
		String response = IOUtils.toString(is);
		System.out.println("response: " + response);
		String expectedBody = "cookieValuecookieValue";
		assertThat(response, containsString(expectedBody));
	}
}
