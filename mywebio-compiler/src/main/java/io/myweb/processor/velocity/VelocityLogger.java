package io.myweb.processor.velocity;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

import javax.annotation.processing.Messager;

import static javax.tools.Diagnostic.Kind;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Logger implementation used internally by Velocity.
 */
public class VelocityLogger implements LogChute {

	private static final boolean LOG_WARN = true;
	private static final boolean LOG_ERROR = true;
	private static final boolean LOG_INFO = true;
	private static final boolean LOG_DEBUG = true;
	private static final boolean LOG_TRACE = true;

	public static final String LOG_PREFIX = "Velocity";

	private final Messager messager;

	public VelocityLogger(Messager messager) {
		this.messager = messager;
	}

	private void log(Kind kind, String msg) {
		messager.printMessage(kind, msg);
	}

	private void log(Kind kind, String msg, Throwable t) {
		messager.printMessage(kind, msg + "\n" + getStackTrace(t));
	}

	@Override
	public void init(RuntimeServices rs) throws Exception {
	}

	@Override
	public void log(int level, String message) {
		switch (level) {
			case LogChute.WARN_ID:
				log(Kind.WARNING, LOG_PREFIX + LogChute.WARN_PREFIX + message);
				break;
			case LogChute.ERROR_ID:
				log(Kind.ERROR, LOG_PREFIX + LogChute.ERROR_PREFIX + message);
				break;
			case LogChute.INFO_ID:
				log(Kind.NOTE, LOG_PREFIX + LogChute.INFO_PREFIX + message);
				break;
			case LogChute.DEBUG_ID:
				log(Kind.NOTE, LOG_PREFIX + LogChute.DEBUG_PREFIX + message);
				break;
			case LogChute.TRACE_ID:
				log(Kind.NOTE, LOG_PREFIX + LogChute.TRACE_PREFIX + message);
				break;
			default:
				logFatal(level);
		}
	}

	@Override
	public void log(int level, String message, Throwable t) {
		switch (level) {
			case LogChute.WARN_ID:
				log(Kind.WARNING, LOG_PREFIX + LogChute.WARN_PREFIX + message, t);
				break;
			case LogChute.ERROR_ID:
				log(Kind.ERROR, LOG_PREFIX + LogChute.ERROR_PREFIX + message, t);
				break;
			case LogChute.INFO_ID:
				log(Kind.NOTE, LOG_PREFIX +  LogChute.INFO_PREFIX + message, t);
				break;
			case LogChute.DEBUG_ID:
				log(Kind.NOTE, LOG_PREFIX + LogChute.DEBUG_PREFIX + message, t);
				break;
			case LogChute.TRACE_ID:
				log(Kind.NOTE, LOG_PREFIX + LogChute.TRACE_PREFIX + message, t);
				break;
			default:
				logFatal(level);
		}
	}

	private void logFatal(int level) {
		log(Kind.ERROR, LOG_PREFIX + " unknown velocity level " + level + " (check your code)");
	}

	@Override
	public boolean isLevelEnabled(int level) {
		switch (level) {
			case LogChute.WARN_ID:
				return LOG_WARN;
			case LogChute.ERROR_ID:
				return LOG_ERROR;
			case LogChute.INFO_ID:
				return LOG_INFO;
			case LogChute.DEBUG_ID:
				return LOG_DEBUG;
			case LogChute.TRACE_ID:
				return LOG_TRACE;
			default:
				logFatal(level);
				return true;
		}
	}
}
