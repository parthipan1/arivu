package org.arivu.dbds;

import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;

public final class TomcatListener implements LifecycleListener {

	public TomcatListener() {
		super();
	}

	@Override
	public void lifecycleEvent(LifecycleEvent e) {
		LifecycleState state = e.getLifecycle().getState();
		if (state == LifecycleState.STOPPING) {
			AbstractDataSource c = null;
			while ((c = AbstractDataSource.instanceQueue.poll()) != null) {
				c.destroy();
			}
		} 
	}

}
