package io.myweb.contest;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends Activity {


    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        register();
    }



    private void register() {
        final String SERVER_ADDRESS = "http://demo0473404.mockable.io/contest";
        final String ip = getIp(this.getApplicationContext());
        final String body = String.format("{\"ip\":\"%s\"}", ip);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    doPost(SERVER_ADDRESS, body);
                } catch (IOException e) {
                    throw new RuntimeException(e);

                }
                return null;
            }
        }.execute();
    }

    private void doPost(String SERVER_ADDRESS, String body) throws IOException {
        HttpPost httpPost = new HttpPost(SERVER_ADDRESS);
        httpPost.setEntity(new ByteArrayEntity(body.getBytes()));
        HttpClient httpClient = new DefaultHttpClient();
        httpPost.setHeader("Content-Type","application/json");
        HttpResponse response = httpClient.execute(httpPost);

        Log.i(TAG,"got http response: "+response.getStatusLine());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        response.getEntity().writeTo(baos);
        Log.i(TAG,"response body: "+baos.toString());
    }


    public static String getIp(Context ctx) {
        WifiManager wifiMgr = (WifiManager) ctx.getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wifiMgr.getConnectionInfo().getIpAddress());
    }
}
