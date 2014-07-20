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

public class HeaderTest extends MywebTestCase {

	public static final String GET_1 = "123456789012345678901234567890123456\n" +
			"GET /headertest HTTP/1.1\r\n" +
			"Host: localhost\r\n" +
			"Cookie: a=1\r\n" +
			"Cookie: b=2\r\n" +
			"Connection: Keep-Alive\r\n\r\n";

	@Test
	public void headersShouldBeDecoded() throws IOException {
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
		String expectedBody = "Host: localhost " +
				"First cookie: a=1 " +
				"All cookies: a=1, b=2";
		assertThat(response, containsString(expectedBody));
	}
}
