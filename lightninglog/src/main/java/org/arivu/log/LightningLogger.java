/**
 * 
 */
package org.arivu.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.log.appender.AppenderProperties;
import org.arivu.log.appender.Appenders;
import org.arivu.log.converter.StringConverter;
import org.arivu.log.queue.Consumer;
import org.arivu.log.queue.Producer;
import org.arivu.utils.Ason;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author P
 *
 */
public final class LightningLogger implements Logger {

	private final String name;
	private transient String shortLogName = null;

	public LightningLogger(String name) {
		super();
		this.name = name;
		this.currrentLogLevel = getCurrrentLogLevel(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isTraceEnabled()
	 */
	@Override
	public boolean isTraceEnabled() {
		return isLevelEnabled(LOG_LEVEL_TRACE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(java.lang.String)
	 */
	@Override
	public void trace(String msg) {
		log(LOG_LEVEL_TRACE, msg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object)
	 */
	@Override
	public void trace(String format, Object param1) {
		formatAndLog(LOG_LEVEL_TRACE, format, param1, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void trace(String format, Object param1, Object param2) {
		formatAndLog(LOG_LEVEL_TRACE, format, param1, param2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(String format, Object... argArray) {
		formatAndLog(LOG_LEVEL_TRACE, format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void trace(String msg, Throwable t) {
		log(LOG_LEVEL_TRACE, msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isTraceEnabled(org.slf4j.Marker)
	 */
	@Override
	public boolean isTraceEnabled(Marker marker) {
		return isTraceEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String)
	 */
	@Override
	public void trace(Marker marker, String msg) {
		trace(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void trace(Marker marker, String format, Object param1) {
		trace(format, param1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker marker, String format, Object param1, Object param2) {
		trace(format, param1, param2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void trace(Marker marker, String format, Object... argArray) {
		trace(format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		trace(msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isDebugEnabled()
	 */
	@Override
	public boolean isDebugEnabled() {
		return isLevelEnabled(LOG_LEVEL_DEBUG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(java.lang.String)
	 */
	@Override
	public void debug(String msg) {
		log(LOG_LEVEL_DEBUG, msg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object)
	 */
	@Override
	public void debug(String format, Object param1) {
		formatAndLog(LOG_LEVEL_DEBUG, format, param1, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void debug(String format, Object param1, Object param2) {
		formatAndLog(LOG_LEVEL_DEBUG, format, param1, param2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(String format, Object... argArray) {
		formatAndLog(LOG_LEVEL_DEBUG, format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void debug(String msg, Throwable t) {
		log(LOG_LEVEL_DEBUG, msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isDebugEnabled(org.slf4j.Marker)
	 */
	@Override
	public boolean isDebugEnabled(Marker marker) {
		return isDebugEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String)
	 */
	@Override
	public void debug(Marker marker, String msg) {
		debug(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void debug(Marker marker, String format, Object param1) {
		debug(format, param1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker marker, String format, Object param1, Object param2) {
		debug(format, param1, param2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void debug(Marker marker, String format, Object... argArray) {
		debug(format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		debug(msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isInfoEnabled()
	 */
	@Override
	public boolean isInfoEnabled() {
		return isLevelEnabled(LOG_LEVEL_INFO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(java.lang.String)
	 */
	@Override
	public void info(String msg) {
		log(LOG_LEVEL_INFO, msg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object)
	 */
	@Override
	public void info(String format, Object arg) {
		formatAndLog(LOG_LEVEL_INFO, format, arg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void info(String format, Object arg1, Object arg2) {
		formatAndLog(LOG_LEVEL_INFO, format, arg1, arg2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void info(String format, Object... argArray) {
		formatAndLog(LOG_LEVEL_INFO, format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void info(String msg, Throwable t) {
		log(LOG_LEVEL_INFO, msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isInfoEnabled(org.slf4j.Marker)
	 */
	@Override
	public boolean isInfoEnabled(Marker marker) {
		return isInfoEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String)
	 */
	@Override
	public void info(Marker marker, String msg) {
		info(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void info(Marker marker, String format, Object arg) {
		info(format, arg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		info(format, arg1, arg2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void info(Marker marker, String format, Object... argArray) {
		info(format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void info(Marker marker, String msg, Throwable t) {
		info(msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isWarnEnabled()
	 */
	@Override
	public boolean isWarnEnabled() {
		return isLevelEnabled(LOG_LEVEL_WARN);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(java.lang.String)
	 */
	@Override
	public void warn(String msg) {
		log(LOG_LEVEL_WARN, msg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object)
	 */
	@Override
	public void warn(String format, Object arg) {
		formatAndLog(LOG_LEVEL_WARN, format, arg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(String format, Object... argArray) {
		formatAndLog(LOG_LEVEL_WARN, format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void warn(String format, Object arg1, Object arg2) {
		formatAndLog(LOG_LEVEL_WARN, format, arg1, arg2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void warn(String msg, Throwable t) {
		log(LOG_LEVEL_WARN, msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isWarnEnabled(org.slf4j.Marker)
	 */
	@Override
	public boolean isWarnEnabled(Marker marker) {
		return isWarnEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String)
	 */
	@Override
	public void warn(Marker marker, String msg) {
		warn(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void warn(Marker marker, String format, Object arg) {
		warn(format, arg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		warn(format, arg1, arg2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void warn(Marker marker, String format, Object... argArray) {
		warn(format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		warn(msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isErrorEnabled()
	 */
	@Override
	public boolean isErrorEnabled() {
		return isLevelEnabled(LOG_LEVEL_ERROR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(java.lang.String)
	 */
	@Override
	public void error(String msg) {
		log(LOG_LEVEL_ERROR, msg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object)
	 */
	@Override
	public void error(String format, Object arg) {
		formatAndLog(LOG_LEVEL_ERROR, format, arg, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void error(String format, Object arg1, Object arg2) {
		formatAndLog(LOG_LEVEL_ERROR, format, arg1, arg2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(String format, Object... argArray) {
		formatAndLog(LOG_LEVEL_ERROR, format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void error(String msg, Throwable t) {
		log(LOG_LEVEL_ERROR, msg, t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#isErrorEnabled(org.slf4j.Marker)
	 */
	@Override
	public boolean isErrorEnabled(Marker marker) {
		return isErrorEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String)
	 */
	@Override
	public void error(Marker marker, String msg) {
		error(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void error(Marker marker, String format, Object arg) {
		error(format, arg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		error(format, arg1, arg2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void error(Marker marker, String format, Object... argArray) {
		error(format, argArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void error(Marker marker, String msg, Throwable t) {
		error(msg, t);
	}

	/**
	 * This is our internal implementation for logging regular
	 * (non-parameterized) log messages.
	 *
	 * @param level
	 *            One of the LOG_LEVEL_XXX constants defining the log level
	 * @param message
	 *            The message itself
	 * @param t
	 *            The exception whose stack trace should be logged
	 */
	private void log(final int level, final String message, final Throwable t) {
		if (!isLevelEnabled(level)) {
			return;
		}

		final StringBuilder buf = new StringBuilder(32);

		// Append date-time if so configured
		if (SHOW_DATE_TIME) {
			if (DATE_TIME_FORMAT_STR != null) {
				buf.append(new SimpleDateFormat(DATE_TIME_FORMAT_STR).format(new Date(System.currentTimeMillis())));
				buf.append(' ');
			} else {
				buf.append(System.currentTimeMillis() - START_TIME);
				buf.append(' ');
			}
		}

		if (!MDC_KEYS.isEmpty()) {
			for (final String key : MDC_KEYS) {
				final String mdcValue = MDC.get(key);
				if (!NullCheck.isNullOrEmpty(mdcValue)) {
					buf.append(mdcValue).append(' ');
				}
			}
		}

		// Append current thread name if so configured
		if (SHOW_THREAD_NAME) {
			buf.append('[');
			buf.append(Thread.currentThread().getName());
			buf.append("] ");
		}

		if (LEVEL_IN_BRACKETS)
			buf.append('[');

		// Append a readable representation of the log level
		switch (level) {
		case LOG_LEVEL_TRACE:
			buf.append("TRACE");
			break;
		case LOG_LEVEL_DEBUG:
			buf.append("DEBUG");
			break;
		case LOG_LEVEL_INFO:
			buf.append("INFO");
			break;
		case LOG_LEVEL_WARN:
			buf.append(WARN_LEVEL_STRING);
			break;
		case LOG_LEVEL_ERROR:
			buf.append("ERROR");
			break;
		default:
		}
		if (LEVEL_IN_BRACKETS)
			buf.append(']');
		buf.append(' ');

		// Append the name of the log instance if so configured
		if (SHOW_SHORT_LOG_NAME) {
			if (shortLogName == null)
				shortLogName = computeShortName();
			buf.append(String.valueOf(shortLogName)).append(" - ");
		} else if (SHOW_LOG_NAME) {
			buf.append(String.valueOf(name)).append(" - ");
		}

		// Append the message
		buf.append(message);

		write(buf, t);

	}

	private void write(StringBuilder buf, Throwable t) {
		if (buf.length() > 0) {
			LOG_PRODUCER.produce(buf.toString());
		}
		if (t != null) {
			LOG_PRODUCER.produce(stackTraceToString(t));
		}
	}

	private String stackTraceToString(Throwable e) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append(AppenderProperties.separator);
		}
		return sb.toString();
	}

	private String computeShortName() {
		return name.substring(name.lastIndexOf(".") + 1);
	}

	/**
	 * For formatted messages, first substitute arguments and then log.
	 *
	 * @param level
	 * @param format
	 * @param arg1
	 * @param arg2
	 */
	private void formatAndLog(int level, String format, Object arg1, Object arg2) {
		if (!isLevelEnabled(level)) {
			return;
		}
		FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
		log(level, tp.getMessage(), tp.getThrowable());
	}

	/**
	 * For formatted messages, first substitute arguments and then log.
	 *
	 * @param level
	 * @param format
	 * @param arguments
	 *            a list of 3 ore more arguments
	 */
	private void formatAndLog(int level, String format, Object... arguments) {
		if (!isLevelEnabled(level)) {
			return;
		}
		FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
		log(level, tp.getMessage(), tp.getThrowable());
	}

	// private static final int LOG_CNT_REFRESH = 10;
	private volatile int logcnt = 0;
	private int currrentLogLevel = 0;

	/**
	 * Is the given log level currently enabled?
	 *
	 * @param logLevel
	 *            is this level enabled?
	 */
	private boolean isLevelEnabled(final int logLevel) {
		// log level are numerically ordered so can use simple numeric
		// comparison
		// logcnt = (logcnt+1)/LOG_CNT_REFRESH;
		logcnt = (logcnt + 1) & 15;

		if (logcnt == 0)
			currrentLogLevel = getCurrrentLogLevel(name);

		return logLevel >= currrentLogLevel;// currentLogLevel;
	}

	private static int getCurrrentLogLevel(final String name) {
		String levelString = recursivelyComputeLevelString(name);
		if (levelString != null) {
			return stringToLevel(levelString);
		} else {
			return stringToLevel(ALL_LOGGER.get("root"));
		}
	}

	private static String recursivelyComputeLevelString(String tempName) {
		// String tempName = name;
		String levelString = null;
		int indexOfLastDot = tempName.length();
		while (levelString == null && indexOfLastDot > -1) {
			tempName = tempName.substring(0, indexOfLastDot);
			levelString = ALL_LOGGER.get(tempName);// getStringProperty(tempName,
													// null);
			indexOfLastDot = String.valueOf(tempName).lastIndexOf(".");
		}
		return levelString;
	}

	private static String beanNameStr = null;

	private static void registerMXBean(final int cnt) {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			beanNameStr = "org.arivu.log:type=" + LightningLogger.class.getSimpleName() + String.valueOf(cnt);
			mbs.registerMBean(getLoggerMXBean(), new ObjectName(beanNameStr));
		} catch (InstanceAlreadyExistsException e) {
			// e.printStackTrace();
			registerMXBean(cnt + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void unRegisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
			} catch (Exception e) {
//				e.printStackTrace();
				System.err.println(e.toString());
			}
		}
	}

	private static LoggerMXBean getLoggerMXBean() {
		return new LoggerMXBean() {

//			@Override
//			public void setShowThreadName(boolean flag) {
////				SHOW_THREAD_NAME = flag;
//			}
//
//			@Override
//			public void setShowLogShortName(boolean flag) {
////				SHOW_SHORT_LOG_NAME = flag;
//			}
//
//			@Override
//			public void setShowLogName(boolean flag) {
////				SHOW_LOG_NAME = flag;
//			}
//
//			@Override
//			public void setShowDate(boolean flag) {
////				SHOW_DATE_TIME = flag;
//			}

			@Override
			public void setLogLevel(String logger, String level) {
				int l = -1;
				if (level != null) {
					l = stringToLevel(level);
					if (l > 0) {
						Object oldVal = ALL_LOGGER.remove(logger);
						ALL_LOGGER.put(logger, level);
						if (oldVal == null) {
							System.err.println(" Logger  " + logger + " set to " + level);
						} else {
							System.err.println(" Logger  " + logger + " set to " + level + " from " + oldVal);
						}
					}
				}
			}

			@Override
			public void setLogFileSize(long size) {
				if (size > 100000) {
					AppenderProperties.FILE_THRESHOLD_LIMIT = size;
				}
			}

			@Override
			public void setLogFile(String file) {
				if (file != null) {
					File f = new File(file);
					if (f.isDirectory()) {
						System.err.println("Log file set with a directory " + file);
					} else if (!f.getParentFile().exists()) {
						System.err.println("Log file set directory " + f.getParentFile() + " doesn't exists!");
					}
					LOG_FILE = file;
				}
			}

//			@Override
//			public void setDateFormat(String format) {
////				if (format != null && "epoch".equalsIgnoreCase(format)) {
////					DATE_TIME_FORMAT_STR = null;
////				} else if (format != null) {
////					try {
////						new SimpleDateFormat(format).format(new Date(System.currentTimeMillis()));
////						DATE_TIME_FORMAT_STR = format;
////					} catch (Exception e) {
////						DATE_TIME_FORMAT_STR = null;
////						System.err.println("Bad date format " + format + "; set thru JMX!");
////					}
////				} else {
////					DATE_TIME_FORMAT_STR = null;
////				}
//			}

			@Override
			public boolean getShowThreadName() {
				return SHOW_THREAD_NAME;
			}

			@Override
			public boolean getShowLogShortName() {
				return SHOW_SHORT_LOG_NAME;
			}

			@Override
			public boolean getShowLogName() {
				return SHOW_LOG_NAME;
			}

			@Override
			public boolean getShowDate() {
				return SHOW_DATE_TIME;
			}

			@Override
			public String getLogLevel(String logger) {
				return intlevelToString(getCurrrentLogLevel(logger));
				// String levelString = recursivelyComputeLevelString(logger);
				// if (levelString != null) {
				// return intlevelToString(stringToLevel(levelString));
				// } else {
				// return
				// intlevelToString(stringToLevel(ALL_LOGGER.get("root")));
				// }
			}

			@Override
			public long getLogFileSize() {
				return AppenderProperties.FILE_THRESHOLD_LIMIT;
			}

			@Override
			public String getLogFile() {
				return LOG_FILE;
			}

			@Override
			public String getDateFormat() {
				return DATE_TIME_FORMAT_STR;
			}

			@Override
			public void flush() throws Exception {
				LOG_PRODUCER.flush();
			}

			@Override
			public void close() throws Exception {
				LOG_PRODUCER.close();
			}
		};
	}

	private static String getEnv(String key, String dvalue) {
		return System.getProperty(key, (System.getenv().get(key) == null ? dvalue : System.getenv().get(key)));
	}

	private static Map<String, Object> loadProperties() {
		InputStream in = null;
		String instr = getEnv(CONFIGURATION_FILE, null);
		if (instr == null) {
			in = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
				public InputStream run() {
					ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
					if (threadCL != null) {
						return threadCL.getResourceAsStream(CONFIGURATION_FILE);
					} else {
						return ClassLoader.getSystemResourceAsStream(CONFIGURATION_FILE);
					}
				}
			});
		} else {
			try {
				in = new FileInputStream(new File(instr));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (null != in) {
			try {
				Map<String, Object> fromJson = new Ason().fromJson(in);
				in.close();
				return fromJson;
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		return null;
	}

	private static Producer<String> LOG_PRODUCER;

	public static void flush() {
		try {
			LOG_PRODUCER.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String intlevelToString(int level) {
		if (LOG_LEVEL_TRACE == level) {
			return "trace";
		} else if (LOG_LEVEL_DEBUG == level) {
			return "debug";
		} else if (LOG_LEVEL_INFO == level) {
			return "info";
		} else if (LOG_LEVEL_WARN == level) {
			return "warn";
		} else if (LOG_LEVEL_ERROR == level) {
			return "error";
		}
		return "info";
	}

	private static int stringToLevel(String levelStr) {
		if ("trace".equalsIgnoreCase(levelStr)) {
			return LOG_LEVEL_TRACE;
		} else if ("debug".equalsIgnoreCase(levelStr)) {
			return LOG_LEVEL_DEBUG;
		} else if ("info".equalsIgnoreCase(levelStr)) {
			return LOG_LEVEL_INFO;
		} else if ("warn".equalsIgnoreCase(levelStr)) {
			return LOG_LEVEL_WARN;
		} else if ("error".equalsIgnoreCase(levelStr)) {
			return LOG_LEVEL_ERROR;
		}
		// assume INFO by default
		return -1;
	}

	private static Collection<String> convert(final Map<String, String> map) {
		if (map != null) {
			Collection<String> arr = new DoublyLinkedList<String>();// new
																	// ArrayList<String>();
			Set<Entry<String, String>> entrySet = map.entrySet();
			for (Entry<String, String> e : entrySet) {
				arr.add(e.getValue());
			}
			return arr;
		}
		return null;
	}

	private static String[] split(String txt, String k) {
		int index = txt.indexOf(k);
		Collection<String> arr = new DoublyLinkedList<String>();// new
																// ArrayList<String>();
		while (index >= 0) {
			arr.add(txt.substring(0, index));
			txt = txt.substring(index + k.length(), txt.length());
			index = txt.indexOf(k);
		}
		arr.add(txt);
		String[] retArr = new String[arr.size()];
		int i = 0;
		for (String s : arr)
			retArr[i++] = s;
		return retArr;
	}

	private static int getInt(final Map<String, Object> json, final String token, final int defailt) {
		Object object = get(json, token, null);
		if(object!=null){
			if(object instanceof Number)
				return ((Number)object).intValue();
			else{
				try {
					return ((Double)Double.parseDouble(object.toString())).intValue();
				} catch (NumberFormatException e) {
					return defailt;
				}
			}
		}
		return defailt;
	}
	
	@SuppressWarnings("unchecked")
	private static Object get(final Map<String, Object> json, final String token, final Object defailt) {
		final String[] split = split(token, ".");
		Map<String, Object> obj = json;

		for (int i = 0; i < split.length; i++) {
			final Object object = obj.get(split[i]);
			if (object == null) {
				break;
			} else if (i == split.length - 1) {
				return object;
			} else {
				try {
					obj = (Map<String, Object>) object;
				} catch (Exception e) {
					break;
				}
			}
		}

		return defailt;
	}

	private static final String CONFIGURATION_FILE = "lightninglog.json";

	private static final long START_TIME = System.currentTimeMillis();
	private static final Map<String, String> ALL_LOGGER = new Amap<String, String>();
	private static final List<String> MDC_KEYS = new DoublyLinkedList<String>();
	private static final int LOG_LEVEL_TRACE = 00;
	private static final int LOG_LEVEL_DEBUG = 10;
	private static final int LOG_LEVEL_INFO = 20;
	private static final int LOG_LEVEL_WARN = 30;
	private static final int LOG_LEVEL_ERROR = 40;

	// private final static int DEFAULT_LOG_LEVEL;
	private static final boolean SHOW_DATE_TIME;
	private static final String DATE_TIME_FORMAT_STR;
	// private static DateFormat DATE_FORMATTER = null;
	private static final boolean SHOW_THREAD_NAME;
	private static final boolean SHOW_LOG_NAME;
	private static final boolean SHOW_SHORT_LOG_NAME;
	public static String LOG_FILE;
	private final static boolean LEVEL_IN_BRACKETS;
	private final static String WARN_LEVEL_STRING;

	static {
		final Map<String, Object> json = loadProperties();

		if (json == null) {
			ALL_LOGGER.put("root", "debug");
			Consumer.RINGBUFFER_LEN = 50;
			Consumer.BATCH_SIZE = 50;
			SHOW_LOG_NAME = true;
			SHOW_SHORT_LOG_NAME = false;
			SHOW_DATE_TIME = false;
			SHOW_THREAD_NAME = false;
			DATE_TIME_FORMAT_STR = "yyyy-MM-dd HH:mm:ss:SSS Z";
			LEVEL_IN_BRACKETS = false;
			WARN_LEVEL_STRING = "WARN";
			LOG_FILE = "lightninglog.log";
			AppenderProperties.FILE_THRESHOLD_LIMIT = 5242880000l;
		} else {
			@SuppressWarnings("unchecked")
			Map<String, String> loggers = (Map<String, String>) get(json, "loggers", null);

			if (loggers != null && loggers.size() > 0) {
				ALL_LOGGER.putAll(loggers);
			}
			Consumer.RINGBUFFER_LEN = getInt(json, "buffer.ring", 300);
			Consumer.BATCH_SIZE = getInt(json, "buffer.batch", 100);

			String mdcKeys = (String) get(json, "log.mdc", null);
			if (!NullCheck.isNullOrEmpty(mdcKeys)) {
				String[] split = mdcKeys.split(",");
				MDC_KEYS.addAll(Arrays.asList(split));
			}

			SHOW_LOG_NAME = (Boolean) get(json, "log.showName", true);
			SHOW_SHORT_LOG_NAME = (Boolean) get(json, "log.showShortName", false);
			SHOW_DATE_TIME = (Boolean) get(json, "log.showDateTime", false);
			SHOW_THREAD_NAME = (Boolean) get(json, "log.showThreadName", false);
			DATE_TIME_FORMAT_STR = (String) get(json, "log.dateTimeFormat", null);
			LEVEL_IN_BRACKETS = false;
			WARN_LEVEL_STRING = "WARN";

			AppenderProperties.FILE_EXT_FORMAT = (String) get(json, "log.fileDateTimeExt",
					AppenderProperties.FILE_EXT_FORMAT);

			LOG_FILE = (String) get(json, "log.file", "lightninglog.log");
			AppenderProperties.FILE_THRESHOLD_LIMIT = ((Number) get(json, "log.fileSize", 5242880)).longValue();
		}
		createProducer(json);
		registerMXBean(0);
	}

	private static void createProducer(final Map<String, Object> json) {
		try {
			LOG_PRODUCER = new Producer<String>(new StringConverter(), getAppenders(json, "file"));
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

				@Override
				public void run() {
					unRegisterMXBean();
					try {
						LOG_PRODUCER.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private static Collection<Appender> getAppenders(Map<String, Object> json, String defaultAppenders)
			throws IOException {
		Collection<String> split = null;

		if (json != null) {
			Object object = get(json, "appenders", null);
			if (object instanceof Collection) {
				split = (Collection<String>) object;
			} else {
				split = (Collection<String>) convert((Map<String, String>) get(json, "appenders", null));
			}
		}
		if (NullCheck.isNullOrEmpty(split))
			split = Arrays.asList(defaultAppenders.split(","));

		Collection<Appender> lws = new DoublyLinkedList<Appender>();
		for (String s : split) {
			Appenders valueOf = Appenders.valueOf(s);
			if (valueOf != null) {
				lws.add(valueOf.get(LOG_FILE));
			} else {
				addCustomAppender(lws, s);
			}
		}

		return lws;
	}

	public static void addCustomAppender(final Collection<Appender> list, final String customAppender) {
		if (customAppender == null) {
			throw new IllegalArgumentException(
					"No class name set in " + CONFIGURATION_FILE + " for custom appender " + customAppender + "!");
		} else {
			try {
				Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(customAppender);
				if (loadClass != null) {
					Object newInstance = loadClass.newInstance();
					if (newInstance instanceof Appender) {
						list.add((Appender) newInstance);
					} else {
						System.err.println("Custom appender class " + customAppender
								+ " not an instance of org.arivu.log.Appender, All custom appenders should implement org.arivu.log.Appender interface.");
					}
				} else {
					System.err.println("Unable to load class " + customAppender + " !");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
