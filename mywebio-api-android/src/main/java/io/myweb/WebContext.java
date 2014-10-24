package io.myweb;

import android.content.ComponentName;
import android.content.Context;

import java.util.List;

public interface WebContext {
	public Context getContext();
	public Object bindService(ComponentName name);
	public List<Endpoint.Info> getEndpointInfos();
	public List<Filter> getFilters();
	public AssetLengthInfo getAssetInfo();
	public RequestProcessor getRequestProcessor();
}
