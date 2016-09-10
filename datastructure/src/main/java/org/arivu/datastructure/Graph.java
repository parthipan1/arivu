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

	DoublyLinkedSet<Node<Identity>> all = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);

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

	public boolean add(final Identity e) throws CyclicException {
		if (e != null) {
			return addInternal(e, true);
		} else {
			return false;
		}
	}

	private boolean addInternal(final Identity e, boolean resolve) throws CyclicException {
		if (e != null) {
			Node<Identity> node = getWrapper(e);
//			final Node<Identity> node = get(e, all, false);
			boolean add = all.add(node);
//			//System.out.println(" added "+e+" add "+add);
			if (add) {
				addAllInternal(e.getParents(), false);
				addAllInternal(e.getChildren(), false);
				if (resolve) {
					resolveAll();
				}
			}
			return add;
		}
		return false;
	}

//	private static final Lock glock = new AtomicWFLock();
	
	private void resolveAll() throws CyclicException {
		
		final DoublyLinkedSet<Node<Identity>> tempAll = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
		for (Node<Identity> node : all) {
			final Node<Identity> wrapper = getWrapper(node.obj);
			if (wrapper != null) {
				tempAll.add(wrapper);
			}
		}

		final Set<Node<Identity>> root = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
		final Set<Node<Identity>> leaf = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
		for (Node<Identity> node : tempAll) {
			setRootLeaf(node, root, leaf, tempAll);
		}
		
		//System.out.println("resolveAll :: "+getStr(root)+" tempAll "+tempAll.size());
		
		recursivelyResolve(tempAll, root, Direction.child, 1);

		for (Node<Identity> node : leaf) {
			if (node.level < 0) {
				recursivelyResolve(tempAll, node, Direction.parent, getMaxLevel()+1);
			}
		}

		all = tempAll;

	}

	private static void recursivelyResolve(final DoublyLinkedSet<Node<Identity>> allNodes,
			final Node<Identity> startNode, final Direction direction, int startLevel) throws CyclicException {
		Set<Node<Identity>> nodes = new DoublyLinkedSet<Graph.Node<Identity>>();
		nodes.add(startNode);
		recursivelyResolve(allNodes, nodes, direction, startLevel);
	}
	
	private static void recursivelyResolve(final DoublyLinkedSet<Node<Identity>> allNodes,
			final Set<Node<Identity>> startNodes, final Direction direction, int startLevel) throws CyclicException {
		//System.out.println(" recursivelyResolve allNodes :: "+getStr(allNodes));
		final Set<Node<Identity>> resolved = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
		final Set<Node<Identity>> unresolved = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
		resolved.addAll(startNodes);
		final Set<Node<Identity>> cursor = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
		cursor.addAll(resolved);
		while (resolved.size() < allNodes.size()) {
			//System.out.println(" recursivelyResolve tempAll cursor :: "+getStr(cursor));
			for (Node<Identity> node : cursor) {
				final Collection<Node<Identity>> children = get(node, allNodes, direction, true);
				if (children != null && children.size() > 0) {
					for (Node<Identity> wrapper : children) {
						final DoublyLinkedSet<Node<Identity>> search = allNodes.search(wrapper);
						if (search != null) {
							search.obj.level = startLevel;
							//System.out.println(" setLevel "+search.obj.level+" "+search.obj.obj+" recursivelyResolve 1");
							unresolved.add(search.obj);
						}else{
							allNodes.add(wrapper);
							wrapper.level = startLevel;
							unresolved.add(wrapper);
							//System.out.println(" setLevel "+wrapper.level+" "+wrapper.obj+" recursivelyResolve 2 unresolved "+getStr(unresolved)+" allNodes :: "+getStr(allNodes));
						}
					}
				}
			}
			cursor.clear();
			final Set<Node<Identity>> tresolved = new DoublyLinkedSet<Node<Identity>>(CompareStrategy.EQUALS);
			tresolved.addAll(resolved);
			tresolved.retainAll(unresolved);

			if (!tresolved.isEmpty())
				throw new CyclicException("Cyclic nodes ( " + getStr(tresolved) + " ) identified!");

			if (unresolved.isEmpty())
				break;
			cursor.addAll(unresolved);
			resolved.addAll(unresolved);
			unresolved.clear();
			startLevel = direction.getNext(startLevel);
		}
		//System.out.println("*******");
	}

	private final static Node<Identity> getWrapper(Identity t) {
		if (t == null) {
			return null;
		}

		final Node<Identity> node = new Node<Identity>(t);
		node.level = Integer.MIN_VALUE;
//		//System.out.println("Create Node "+t);
		return node;
	}

	private final static String getStr(Set<Node<Identity>> tresolved) {
		StringBuffer sb = new StringBuffer();

		for (Node<Identity> node : tresolved) {
			if (sb.length() == 0) {
				sb.append(node.obj.toString()).append("::").append(node.level);
			} else {
				sb.append(",").append(node.obj.toString()).append("::").append(node.level);
			}
		}

		return sb.toString();
	}

	private final static DoublyLinkedSet<Node<Identity>> get(Node<Identity> node,
			DoublyLinkedSet<Node<Identity>> tempAll, Direction dir, boolean includeAll) {
		DoublyLinkedSet<Node<Identity>> set = new DoublyLinkedSet<Graph.Node<Identity>>(CompareStrategy.EQUALS);
		final Collection<? extends Identity> cols = dir.get(node.obj);
		if (cols != null && cols.size() >= 0) {
			for (Identity p : cols) {
				final Node<Identity> wrapper = get(p, tempAll, true);
				if (wrapper != null) {
					set.add(wrapper);
				}
			}
		}
		if (includeAll) {
			final Direction other = dir.getOther();
			for (Node<Identity> n : tempAll) {
				if (node != n && get(n, tempAll, other, false).contains(node)) {
					set.add(n);
				}
			}
		}
		return set;
	}

	private final static Node<Identity> get(Identity p, DoublyLinkedSet<Node<Identity>> tempAll, boolean add) {
		if (p != null) {
			final DoublyLinkedSet<Node<Identity>> search = tempAll.search(p);
			if (search == null) {
				final Node<Identity> wrapper = getWrapper(p);
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

	private static void setRootLeaf(final Node<Identity> node, final Set<Node<Identity>> root,
			final Set<Node<Identity>> leaf, final DoublyLinkedSet<Node<Identity>> tempAll) {
		final Collection<Node<Identity>> parents = get(node, tempAll, Direction.parent, true);
		if (parents.size() == 0) {
			node.level = 0;
			//System.out.println(" setLevel "+node.level+" "+node.obj+" setRootLeaf");
			root.add(node);
		}
		final Collection<Node<Identity>> children = get(node, tempAll, Direction.child, true);
		if (children != null && children.size() >= 0) {
			if (children.size() == 0) {
				leaf.add(node);
			}
		}
	}
	
	public void resolve() throws CyclicException{
		resolveAll();
	}
	
	public boolean remove(final Object o) {
		try {
			return all.remove(getWrapper((Identity) o));
		} catch (ClassCastException e) {
		}
		return false;
	}

	public boolean addAll(final Collection<? extends Identity> c) throws CyclicException {
		return addAllInternal(c, true);
	}

	private boolean addAllInternal(final Collection<? extends Identity> c, boolean resolve) throws CyclicException {
		boolean r = true;
		if (c != null) {
			for (Identity e : c) {
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
		for (Node<Identity> n : all) {
			if (n != null)
				ml = Math.max(ml, n.level);
		}
		return ml;
	}

	public Collection<Identity> get(final int level) {
		final List<Identity> l = new DoublyLinkedList<Identity>();
		for (Node<Identity> n : all) {
			if (n != null && n.level == level) {
				l.add(n.obj);
			}
		}
		return Collections.unmodifiableList(l);
	}

	enum Direction {
		parent {

			@Override
			Collection<? extends Identity> get(Identity i) {
				return i.getParents();
			}

			@Override
			int getNext(int l) {
				return l - 1;
			}

		},
		child;

		int getNext(int l) {
			return l + 1;
		}

		Collection<? extends Identity> get(Identity i) {
			return i.getChildren();
		}

		Direction getOther() {
			if (this == child)
				return Direction.parent;
			else
				return Direction.child;
		}
	}

}
