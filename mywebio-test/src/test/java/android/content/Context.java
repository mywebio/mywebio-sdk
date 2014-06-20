package android.content;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;

public abstract class Context {

	public AssetManager getAssets() {
		return new AssetManager();
	}

	public String getPackageName() {
		return "pkg";
	}

	public PackageManager getPackageManager() {
		return new PackageManager();
	}

	public ApplicationInfo getApplicationInfo() {
		return new ApplicationInfo();
	}
}
