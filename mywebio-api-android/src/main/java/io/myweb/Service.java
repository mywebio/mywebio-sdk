package io.myweb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.myweb.http.Method;

public class Service extends LocalService<WebContext> implements WebContext {
	static final String ASSETS_CLASS_NAME = "io.myweb.MyAssetInfo";
	static final String SERVICES_CLASS_NAME = "io.myweb.MyServices";

	public static final String TAG = Service.class.getName();

	private static List<Endpoint.Info> endpointList = new ArrayList<Endpoint.Info>();
	private static List<Filter> filterList = new ArrayList<Filter>();

	static {
		addEndpointInfo(new Endpoint.Info(Method.GET, AppInfoEndpoint.SERVICES_JSON, AppInfoEndpoint.class));
		addEndpointInfo(new Endpoint.Info(Method.GET, "/", AssetEndpoint.class));
		try {
			// run static initializers for generated classes
			Class.forName(SERVICES_CLASS_NAME);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		filterList = Collections.unmodifiableList(filterList);
		endpointList = Collections.unmodifiableList(endpointList);
	}

	// package access
	static void addEndpointInfo(Endpoint.Info info) {
		endpointList.add(info);
	}

	static void addFilter(Filter filter) {
		filterList.add(filter);
	}

	private AssetLengthInfo assetLengthInfo;

	private ExecutorService executorService;

	private RequestProcessor requestProcessor;

	private LocalServer localServer;

	@Override
	public Context getContext() {
		return this;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received intent from server");
		// We have to start listening on local socket
		if (executorService == null) {
			executorService = new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(), ThreadFactories.newWorkerThreadFactory());
		}
		if (localServer == null || localServer.isClosed()) {
			localServer = new LocalServer(this, executorService);
			executorService.submit(localServer);
		}
		return android.app.Service.START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Creating myweb service");
		if (requestProcessor == null) {
			requestProcessor = new RequestProcessor(createEndpoints(), getFilters());
		}
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Destroying myweb service");
		TempServiceConnection.terminateAll();
		if (localServer != null) {
			localServer.shutdown();
			localServer = null;
		}
		if (executorService != null) {
			executorService.shutdown();
			executorService = null;
		}
	}

	public Object bindService(ComponentName name) {
		TempServiceConnection connection = TempServiceConnection.get(name.getClassName());
		if (connection == null) connection = new TempServiceConnection(this, name, executorService);
		return connection.getServiceObject(); // awaits connection
	}

	public List<Endpoint.Info> getEndpointInfos() {
		return endpointList;
	}

	public List<Filter> getFilters() {
		return filterList;
	}

	public AssetLengthInfo getAssetInfo() {
		if (assetLengthInfo == null) {
			try {
				assetLengthInfo = (AssetLengthInfo) Class.forName(ASSETS_CLASS_NAME).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return assetLengthInfo;
	}

	private List<? extends Endpoint> createEndpoints() {
		List<Endpoint> list = new LinkedList<Endpoint>();
		for (Endpoint.Info info: endpointList) {
			try {
				Endpoint ep = (Endpoint) info.getImplementingClass().getConstructor(WebContext.class).newInstance(this);
				System.out.println(ep.httpMethod().toString()+" "+ep.originalPath()+" instantiated!");
				list.add(ep);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	public RequestProcessor getRequestProcessor() {
		return requestProcessor;
	}
}

