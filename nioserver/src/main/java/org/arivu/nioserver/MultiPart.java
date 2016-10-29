package org.arivu.nioserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

import org.arivu.utils.Utils;

public final class MultiPart {
	final Map<String, String> headers;
	final List<ByteData> body;

	final String name, filename, contentType, contentDisposition;

	/**
	 * @param headers
	 * @param body
	 * @param name
	 * @param filename
	 * @param contentType
	 * @param contentDisposition
	 */
	MultiPart(Map<String, String> headers, List<ByteData> body, String name, String filename, String contentType,
			String contentDisposition) {
		super();
		this.headers = headers;
		this.body = body;
		this.name = name;
		this.filename = filename;
		this.contentType = contentType;
		this.contentDisposition = contentDisposition;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public List<ByteData> getBody() {
		return Utils.unmodifiableList(body) ;
	}

	public String getName() {
		return name;
	}

	public String getFilename() {
		return filename;
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	public void writeTo(File file, boolean append) throws IOException {
		FileOutputStream fileOutputStream = null;
		FileChannel channel = null;
		try {
			fileOutputStream = new FileOutputStream(file, append);
			channel = fileOutputStream.getChannel();
			for (ByteData bb : body) {
				channel.write(ByteBuffer.wrap(bb.array()));
			}
		}finally {
			if(channel!=null)
				channel.close();
			if(fileOutputStream!=null)
				fileOutputStream.close();
		}
	}

}
