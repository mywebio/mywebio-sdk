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

	public static final String GET_TEST1 = "123456789012345678901234567890123456\n" +
			"GET /test HTTP/1.1\r\n" +
			"Host: localhost\r\n" +
			"Connection: Keep-Alive\r\n";

	@Test
	public void simplestGet() throws IOException, InterruptedException {
		// given
		compile(Test1.class);
		Service service = new Service();
		service.onCreate();
		service.onStartCommand(new Intent(), 0, 0);

		// when
		LocalSocket clientSocket = new LocalSocket();
		clientSocket.connect(new LocalSocketAddress("doesn't matter now"));
		OutputStream os = clientSocket.getOutputStream();
		os.write(GET_TEST1.getBytes());
		os.flush();
		os.close();

		// then
		InputStream is = clientSocket.getInputStream();
		String response = IOUtils.toString(is);
		System.out.println(response);
		String expectedBody = new Test1().test();
		assertThat(response, containsString(expectedBody));
	}
}
