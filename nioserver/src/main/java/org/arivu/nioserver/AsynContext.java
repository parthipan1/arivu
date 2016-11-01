package org.arivu.nioserver;

public interface AsynContext {
	void setAsynchronousFinish(boolean flag);
	boolean isAsynchronousFinish();
	void finish();
}
