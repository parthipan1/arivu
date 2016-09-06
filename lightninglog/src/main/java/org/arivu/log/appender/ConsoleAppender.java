package org.arivu.log.appender;

import java.io.Console;
import java.io.PrintWriter;

import org.arivu.log.Appender;

final class ConsoleAppender implements Appender{
	final PrintWriter writer;
	public ConsoleAppender() {
		super();
		Console console = System.console();
		if( console != null )
			writer = console.writer();
		else
			writer = new PrintWriter(System.out);
	}

	@Override
	public void append(String log) {
		writer.println(log);
	}

	@Override
	public void close() throws Exception {
		writer.close();
	}

}
