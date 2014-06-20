package android.util;

import org.apache.commons.lang3.exception.ExceptionUtils;

public final class Log {

	public static int v(java.lang.String tag, java.lang.String msg) {
		println(0, tag, msg);
		return 0;
	}

	public static int v(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		println(0, tag, msg + "\n"+ getStackTraceString(tr));
		return 0;
	}

	public static int d(java.lang.String tag, java.lang.String msg) {
		println(0, tag, msg);
		return 0;
	}

	public static int d(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		println(0, tag, msg + "\n"+ getStackTraceString(tr));
		return 0;
	}

	public static int i(java.lang.String tag, java.lang.String msg) {
		println(0, tag, msg);
		return 0;
	}

	public static int i(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		println(0, tag, msg + "\n"+ getStackTraceString(tr));
		return 0;
	}

	public static int w(java.lang.String tag, java.lang.String msg) {
		println(0, tag, msg);
		return 0;
	}

	public static int w(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		println(0, tag, msg + "\n"+ getStackTraceString(tr));
		return 0;
	}

	public static int w(java.lang.String tag, java.lang.Throwable tr) {
		println(0, tag, getStackTraceString(tr));
		return 0;
	}

	public static int e(java.lang.String tag, java.lang.String msg) {
		println(0, tag, msg);
		return 0;
	}

	public static int e(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		println(0, tag, msg + "\n"+ getStackTraceString(tr));
		return 0;
	}

	public static int wtf(java.lang.String tag, java.lang.String msg) {
		println(0, tag, msg);
		return 0;
	}

	public static int wtf(java.lang.String tag, java.lang.Throwable tr) {
		println(0, tag, getStackTraceString(tr));
		return 0;
	}

	public static int wtf(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		println(0, tag, msg + "\n"+ getStackTraceString(tr));
		return 0;
	}

	public static java.lang.String getStackTraceString(java.lang.Throwable tr) {
		return ExceptionUtils.getStackTrace(tr);
	}

	public static int println(int priority, java.lang.String tag, java.lang.String msg) {
		System.out.println(tag + " " + msg);
		return 0;
	}

}
