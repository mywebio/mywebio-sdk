package android.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class Log {

	private static final Logger log = LoggerFactory.getLogger(Log.class);

	public static int v(java.lang.String tag, java.lang.String msg) {
		log.debug("<{}> {}", tag, msg);
		return 0;
	}

	public static int v(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		log.debug("<" + tag + "> " + msg, tr);
		return 0;
	}

	public static int d(java.lang.String tag, java.lang.String msg) {
		log.debug("<{}> {}", tag, msg);
		return 0;
	}

	public static int d(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		log.debug("<" + tag + "> " + msg, tr);
		return 0;
	}

	public static int i(java.lang.String tag, java.lang.String msg) {
		log.info("<{}> {}", tag, msg);
		return 0;
	}

	public static int i(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		log.info("<" + tag + "> " + msg, tr);
		return 0;
	}

	public static int w(java.lang.String tag, java.lang.String msg) {
		log.warn("<{}> {}", tag, msg);
		return 0;
	}

	public static int w(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		log.warn("<" + tag + "> " + msg, tr);
		return 0;
	}

	public static int w(java.lang.String tag, java.lang.Throwable tr) {
		log.warn(tag, tr);
		return 0;
	}

	public static int e(java.lang.String tag, java.lang.String msg) {
		log.error("<{}> {}", tag, msg);
		return 0;
	}

	public static int e(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		log.error("<" + tag + "> " + msg, tr);
		return 0;
	}

	public static int wtf(java.lang.String tag, java.lang.String msg) {
		log.error("<{}> WTF {}", tag, msg);
		return 0;
	}

	public static int wtf(java.lang.String tag, java.lang.Throwable tr) {
		log.error("<" + tag + "> WTF ", tr);
		return 0;
	}

	public static int wtf(java.lang.String tag, java.lang.String msg, java.lang.Throwable tr) {
		log.error("<" + tag + "> WTF " + msg, tr);
		return 0;
	}

	public static String getStackTraceString(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		return sw.toString();
	}
}
