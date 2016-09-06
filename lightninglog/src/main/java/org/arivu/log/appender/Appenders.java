package org.arivu.log.appender;

import java.io.IOException;

import org.arivu.log.Appender;

public enum Appenders {
	console, rollingfile {

		@Override
		public Appender get(String name) throws IOException {
			return new RollingFileAppender(name);
		}

	},
	file {

		@Override
		public Appender get(String name) throws IOException {
			return new FileAppender(name);
		}

	},
	zip {

		@Override
		public Appender get(String name) throws IOException {
			return new ZipFileAppender(name);
		}

	},
	rollingzip {

		@Override
		public Appender get(String name) throws IOException {
			return new RollingZipFileAppender(name);
		}

	},
	no {

		@Override
		public Appender get(String name) throws IOException {
			return new Appender() {

				@Override
				public void close() throws Exception {

				}

				@Override
				public void append(String log) {

				}
			};
		}

	};

	public Appender get(String name) throws IOException {
		return new ConsoleAppender();
	}
}
