/**
 * 
 */
package org.arivu.datastructure;

import java.util.Queue;
import java.util.Set;

/**
 * @author P
 *
 */
public interface Aset<E> extends Set<E>, Queue<E> {
	long sizeL();
}
