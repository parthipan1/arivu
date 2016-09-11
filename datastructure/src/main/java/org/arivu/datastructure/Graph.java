/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author P
 *
 */
public final class Graph implements Serializable {

	public static class CyclicException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1771937291580555597L;

		/**
		 * 
		 */
		public CyclicException() {
			super();
		}

		/**
		 * @param message
		 * @param cause
		 * @param enableSuppression
		 * @param writableStackTrace
		 */
		public CyclicException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		/**
		 * @param message
		 * @param cause
		 */
		public CyclicException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public CyclicException(String message) {
			super(message);
		}

		/**
		 * @param cause
		 */
		public CyclicException(Throwable cause) {
			super(cause);
		}

	}

	/**
	 * @author P
	 *
	 */
	public interface Edges {
		Collection<Object> in(Object obj);

		Collection<Object> out(Object obj);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -262978106533357393L;

	private static class Node<T extends Object> implements Serializable {
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

	private DoublyLinkedSet<Node<Object>> all = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);

	private transient Edges edges;

	/**
	 * 
	 */
	public Graph(Edges edges) {
		this.edges = edges;
	}

	public Edges getEdges() {
		return edges;
	}

	public void setEdges(Edges edges) {
		this.edges = edges;
	}

	public int size() {
		return all.size();
	}

	public boolean isEmpty() {
		return all.isEmpty();
	}

	public boolean add(final Object e) throws CyclicException {
		if (e != null) {
			return addInternal(e, true);
		} else {
			return false;
		}
	}

	private boolean addInternal(final Object e, boolean resolve) throws CyclicException {
		if (e != null) {
			Node<Object> node = getWrapper(e);
			// final Node<Object> node = get(e, all, false);
			boolean add = all.add(node);
			// //System.out.println(" added "+e+" add "+add);
			if (add) {
				addAllInternal(edges.in(e), false);
				addAllInternal(edges.out(e), false);
				if (resolve) {
					resolveAll();
				}
			}
			return add;
		}
		return false;
	}

	// private static final Lock glock = new AtomicWFLock();

	private void resolveAll() throws CyclicException {

//		final DoublyLinkedSet<Node<Object>> tempAll = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
//		for (Node<Object> node : all) {
//			final Node<Object> wrapper = getWrapper(node.obj);
//			if (wrapper != null) {
//				tempAll.add(wrapper);
//			}
//		}

		final Set<Node<Object>> head = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
		final Set<Node<Object>> leg = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
		for (Node<Object> node : all) {
			headOverHeels(node, head, leg, all, edges);
		}

		// System.out.println("resolveAll :: "+getStr(root)+" tempAll
		// "+tempAll.size());

		recursivelyResolve(all, head, Direction.out, 1, edges);

		for (Node<Object> node : leg) {
			if (node.level < 0) {
				recursivelyResolve(all, node, Direction.in, getMaxLevel() + 1, edges);
			}
		}

//		all = tempAll;

	}

	private static void filter(final DoublyLinkedSet<Node<Object>> allNodes, final Set<Node<Object>> set,
			Direction direction, Edges edges) {
		final Set<Node<Object>> tset = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
		tset.addAll(set);
		for (Node<Object> node : set) {
			tset.removeAll(get(node, allNodes, direction, true, edges));
		}
		set.clear();
		set.addAll(tset);
		tset.clear();
	}

	private static void recursivelyResolve(final DoublyLinkedSet<Node<Object>> allNodes, final Node<Object> startNode,
			final Direction direction, int startLevel, Edges edges) throws CyclicException {
		Set<Node<Object>> nodes = new DoublyLinkedSet<Graph.Node<Object>>();
		nodes.add(startNode);
		recursivelyResolve(allNodes, nodes, direction, startLevel, edges);
	}

	private static void recursivelyResolve(final DoublyLinkedSet<Node<Object>> allNodes,
			final Set<Node<Object>> startNodes, final Direction direction, int startLevel, Edges edges)
					throws CyclicException {
		// System.out.println(" recursivelyResolve allNodes ::
		// "+getStr(allNodes));
		final Set<Node<Object>> resolved = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
		final Set<Node<Object>> unresolved = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
		resolved.addAll(startNodes);
		final Set<Node<Object>> cursor = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
		cursor.addAll(resolved);
		while (resolved.size() < allNodes.size()) {
			// System.out.println(" recursivelyResolve tempAll cursor ::
			// "+getStr(cursor));
			for (Node<Object> node : cursor) {
				final Collection<Node<Object>> children = get(node, allNodes, direction, true, edges);
				if (children != null && children.size() > 0) {
					for (Node<Object> wrapper : children) {
						final DoublyLinkedSet<Node<Object>> search = allNodes.search(wrapper);
						if (search != null) {
							search.obj.level = startLevel;
							// System.out.println(" setLevel
							// "+search.obj.level+" "+search.obj.obj+"
							// recursivelyResolve 1");
							unresolved.add(search.obj);
						} else {
							allNodes.add(wrapper);
							wrapper.level = startLevel;
							unresolved.add(wrapper);
							// System.out.println(" setLevel "+wrapper.level+"
							// "+wrapper.obj+" recursivelyResolve 2 unresolved
							// "+getStr(unresolved)+" allNodes ::
							// "+getStr(allNodes));
						}
					}
				}
			}
			filter(allNodes, unresolved, direction, edges);
			final Set<Node<Object>> tresolved = new DoublyLinkedSet<Node<Object>>(CompareStrategy.EQUALS);
			tresolved.addAll(resolved);
			tresolved.retainAll(unresolved);
			if (!tresolved.isEmpty())
				throw new CyclicException("Cyclic nodes ( " + getStr(tresolved) + " ) identified!");

			cursor.clear();
			if (unresolved.isEmpty())
				break;
			cursor.addAll(unresolved);
			resolved.addAll(unresolved);
			unresolved.clear();
			startLevel = direction.getNext(startLevel);
		}
		// System.out.println("*******");
	}

	@SuppressWarnings("unchecked")
	private final static Node<Object> getWrapper(Object t) {
		if (t == null) {
			return null;
		}else if( t instanceof Node ){
			return (Node<Object>)t;
		}

		final Node<Object> node = new Node<Object>(t);
		node.level = Integer.MIN_VALUE;
		// //System.out.println("Create Node "+t);
		return node;
	}

	private final static String getStr(Collection<Node<Object>> tresolved) {
		StringBuffer sb = new StringBuffer();

		for (Node<Object> node : tresolved) {
			if (sb.length() == 0) {
				sb.append(node.obj.toString()).append("::").append(node.level);
			} else {
				sb.append(",").append(node.obj.toString()).append("::").append(node.level);
			}
		}

		return sb.toString();
	}

	private final static DoublyLinkedSet<Node<Object>> get(Node<Object> node, DoublyLinkedSet<Node<Object>> tempAll,
			Direction dir, boolean includeAll, Edges edges) {
		DoublyLinkedSet<Node<Object>> set = new DoublyLinkedSet<Graph.Node<Object>>(CompareStrategy.EQUALS);
		final Collection<? extends Object> cols = dir.get(node.obj, edges);
		if (cols != null && cols.size() >= 0) {
			for (Object p : cols) {
				final Node<Object> wrapper = get(p, tempAll, true);
				if (wrapper != null) {
					set.add(wrapper);
				}
			}
		}
		if (includeAll) {
			final Direction other = dir.getOther();
			for (Node<Object> n : tempAll) {
				if (node != n && get(n, tempAll, other, false, edges).contains(node)) {
					set.add(n);
				}
			}
		}
		return set;
	}

	private final static Node<Object> get(Object p, DoublyLinkedSet<Node<Object>> tempAll, boolean add) {
		if (p != null) {
			final DoublyLinkedSet<Node<Object>> search = tempAll.search(p);
			if (search == null) {
				final Node<Object> wrapper = getWrapper(p);
				if (add) {
					tempAll.add(wrapper);
				}
				return wrapper;
			} else {
				return search.obj;
			}
		} else {
			return null;
		}
	}

	private static void headOverHeels(final Node<Object> node, final Set<Node<Object>> root,
			final Set<Node<Object>> leaf, final DoublyLinkedSet<Node<Object>> tempAll, Edges edges) {
		final Collection<Node<Object>> parents = get(node, tempAll, Direction.in, true, edges);
		if (parents.size() == 0) {
			node.level = 0;
			// System.out.println(" setLevel "+node.level+" "+node.obj+"
			// setRootLeaf");
			root.add(node);
		}
		final Collection<Node<Object>> children = get(node, tempAll, Direction.out, true, edges);
		if (children != null && children.size() >= 0) {
			if (children.size() == 0) {

				if (node.level != 0)
					node.level = Integer.MIN_VALUE;

				leaf.add(node);
			}
		}
	}

	public void resolve() throws CyclicException {
		resolveAll();
	}

	public boolean remove(final Object o) {
		try {
			if( o instanceof Node ){
				return all.remove(o);
			}else{
				return all.remove(getWrapper((Object) o));
			}
		} catch (ClassCastException e) {
		}
		return false;
	}

	public boolean addAll(final Collection<? extends Object> c) throws CyclicException {
		return addAllInternal(c, true);
	}

	private boolean addAllInternal(final Collection<? extends Object> c, boolean resolve) throws CyclicException {
		boolean r = true;
		if (c != null) {
			for (Object e : c) {
				r = r & addInternal(e, resolve);
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
		for (Node<Object> n : all) {
			if (n != null)
				ml = Math.max(ml, n.level);
		}
		return ml;
	}

	public Collection<Object> get(final int level) {
		final List<Object> l = new DoublyLinkedList<Object>();
		for (Node<Object> n : all) {
			if (n != null && n.level == level) {
				l.add(n.obj);
			}
		}
		return Collections.unmodifiableList(l);
	}

	void print() {
		int m = getMaxLevel();
		for (int i = 0; i <= m; i++) {
			System.out.println("LEVEL ****** " + i);
			System.out.println("   " + getStrt(get(i)));
		}
		System.out.println("COMPLETE ****** ");
	}

	private final static String getStrt(Collection<Object> tresolved) {
		StringBuffer sb = new StringBuffer();

		for (Object node : tresolved) {
			if (sb.length() == 0) {
				sb.append(node.toString());
			} else {
				sb.append(",").append(node.toString());
			}
		}

		return sb.toString();
	}

	enum Direction {
		in {

			@Override
			Collection<? extends Object> get(Object i, Edges edges) {
				return edges.in(i);
			}

			@Override
			int getNext(int l) {
				return l - 1;
			}

		},
		out;

		int getNext(int l) {
			return l + 1;
		}

		Collection<? extends Object> get(Object i, Edges edges) {
			return edges.out(i);
		}

		Direction getOther() {
			if (this == out)
				return Direction.in;
			else
				return Direction.out;
		}
	}

}
