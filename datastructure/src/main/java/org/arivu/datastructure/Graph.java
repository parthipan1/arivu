/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author P
 *
 */
public final class Graph<T extends Graph.Identity> implements Serializable {
	/**
	 * @author P
	 *
	 */
	public interface Identity {
		Collection<? extends Identity> getChildren();

		Collection<? extends Identity> getParents();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -262978106533357393L;

	private static class Node<T extends Identity> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1347041353565347216L;
		T obj;
		int level = 0;

		Node(T obj) {
			super();
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
			return result;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Node other = (Node) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			return true;
		}

	}

	final DoublyLinkedSet<Node<T>> all = new DoublyLinkedSet<Node<T>>();

	private Node<T> getWrapper(T t) {
		return new Node<T>(t);
	}

	/**
	 * 
	 */
	public Graph() {
	}

	public int size() {
		return all.size();
	}

	public boolean isEmpty() {
		return all.isEmpty();
	}

	public boolean add(final T e) {
		if (e != null) {
			Node<T> node = getWrapper(e);
			boolean add = all.add(node);
			if (add) {
				resolve(node, 0);
			}
			return add;
		} else {
			return false;
		}
	}

	private void resolve(final Node<T> node, final int cnt) {
		final Collection<? extends Identity> parents = node.obj.getParents();
		final Collection<? extends Identity> children = node.obj.getChildren();
		int upperLevel = 0;
		int pc = 0;
		int lowerLevel = 0;
		int cc = 0;

		if (parents != null && parents.size() > 0) {
			pc = parents.size();
			for (Identity p : parents) {
				DoublyLinkedSet<Node<T>> search = all.search(new Node<Identity>(p));
				if (search != null) {
					upperLevel = Math.max(upperLevel, search.obj.level);
				}
			}
		}

		if (children != null && children.size() > 0) {
			cc = children.size();
			for (Identity p : children) {
				DoublyLinkedSet<Node<T>> search = all.search(new Node<Identity>(p));
				if (search != null) {
					lowerLevel = Math.min(lowerLevel, search.obj.level);
				}
			}
		}

		if (cnt > 2) {
			throw new RuntimeException("Unable to resolve " + node.obj);
		} else {
			if (pc == 0 && cc == 0) {
				// do nothing...
			} else if (cc == 0 && pc > 0) {
				node.level = upperLevel + 1;
			} else if (cc > 0 && pc == 0) {
				if (lowerLevel == 0) {
//					reresolve(node, children, cnt);
					// do nothing...
				} else {
					node.level = lowerLevel - 1;
				}
			} else {
				if (lowerLevel <= upperLevel) {
					reresolve(node, children, cnt);
				} else {
					node.level = lowerLevel;
				}
			}
		}

	}

	private void reresolve(final Node<T> node, final Collection<? extends Identity> children, int cnt) {
		if (children != null && children.size() > 0) {
			for (Identity p : children) {
				DoublyLinkedSet<Node<T>> search = all.search(new Node<Identity>(p));
				if (search != null) {
					resolve(search.obj, cnt);
				}
			}
		}
		resolve(node, cnt+1);
	}

	@SuppressWarnings("unchecked")
	public boolean remove(final Object o) {
		try {
			return all.remove(getWrapper((T) o));
		} catch (ClassCastException e) {
		}
		return false;
	}

	public boolean addAll(final Collection<? extends T> c) {
		boolean r = true;
		if (c != null) {
			for (T e : c) {
				r = r & add(e);
			}
		}
		return r;
	}

	public boolean removeAll(final Collection<?> c) {
		boolean r = true;
		if (c != null) {
			for (Object e : c) {
				r = r & remove(e);
			}
		}
		return r;
	}

	public void clear() {
		all.clear();
	}

	public int getMaxLevel() {
		int ml = 0;
		for (Node<T> n : all) {
			if (n != null)
				ml = Math.max(ml, n.level);
		}
		return ml;
	}

	public Collection<T> get(final int level) {
		final List<T> l = new DoublyLinkedList<T>();
		for (Node<T> n : all) {
			if (n != null && n.level == level) {
				l.add(n.obj);
			}
		}
		return Collections.unmodifiableList(l);
	}

}
