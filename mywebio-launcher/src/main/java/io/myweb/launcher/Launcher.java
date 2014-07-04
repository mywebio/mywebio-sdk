package io.myweb.launcher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import io.myweb.api.GET;
import io.myweb.api.Produces;
import io.myweb.api.HttpResponse;

public class Launcher {

    private final static String SERVER_PACKAGE = "io.myweb.server.alpha";

    @GET("/icon/:appName")
    @Produces("image/png")
    public HttpResponse getIcon(Context context, String appName) throws PackageManager.NameNotFoundException {
        BitmapDrawable icon = (BitmapDrawable) context.getPackageManager().getApplicationIcon(appName);
        return HttpResponse.ok().withBody(getIconInputStream(icon));
    }

    @GET("/apps")
    public HttpResponse getApps(Context context) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("apps", getJsonApps(context, context.getPackageManager()));
        return HttpResponse.ok().withBody(response);
    }

    private ByteArrayInputStream getIconInputStream(BitmapDrawable icon) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        icon.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private JSONArray getJsonApps(Context context, PackageManager packageManager) throws JSONException {
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_SERVICES);
        JSONArray jsonApps = new JSONArray();
        for (ApplicationInfo app : apps) {
            if (hasRequestPermission(packageManager, app) && notLauncherOrServerApp(context, app)) {
                jsonApps.put(buildJsonApp(packageManager, app));
            }
        }
        return jsonApps;
    }

    private JSONObject buildJsonApp(PackageManager packageManager, ApplicationInfo app) throws JSONException {
        JSONObject jsonApp = new JSONObject();
        jsonApp.put("name", app.loadLabel(packageManager));
        jsonApp.put("package", app.packageName);
        return jsonApp;
    }

    private boolean notLauncherOrServerApp(Context context, ApplicationInfo app) {
        return !(context.getPackageName().equals(app.packageName) || SERVER_PACKAGE.equals(app.packageName));
    }

    private boolean hasRequestPermission(PackageManager packageManager, ApplicationInfo app) {
        return PackageManager.PERMISSION_GRANTED == packageManager.checkPermission(Manifest.permission.REQUEST, app.packageName);
    }
}
