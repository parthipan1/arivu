/**
 * 
 */
package org.arivu.datastructure;

import java.util.Set;

import org.arivu.datastructure.primitive.DoublyLinkedSetInt;


/**
 * @author P
 *
 */
public final class Tries {

	static class Node {
		final Node parent;
		final char c;
		boolean isLast = false;
		final DoublyLinkedSet<Node> nodes = new DoublyLinkedSet<Tries.Node>();
		final DoublyLinkedSetInt indexes = new DoublyLinkedSetInt();

		public Node(char c, Node p) {
			super();
			this.c = c;
			this.parent = p;
		}

		Node search(final char ic) {
			DoublyLinkedSet<Node> ref = nodes;
			while (ref != null) {
				if (ref.obj != null && ref.obj.c == ic) {
					return ref.obj;
				}
				ref = ref.right;
				if (ref == nodes) {
					break;
				}
			}
			return null;
		}

		Node add(final char ic, int idx) {
			Node search = search(ic);
			if (search == null) {
				Node nn = new Node(ic, this);
				nn.indexes.add(idx);
				nodes.add(nn);
				return nn;
			} else {
				search.indexes.add(idx);
				return search;
			}
		}

		String getPrefix() {
			StringBuffer sb = new StringBuffer();
			sb.append(c);
			Node n = this.parent;
			while (n.parent != null) {
				sb.append(n.c);
				n = n.parent;
			}
			return sb.reverse().toString();
		}
	}

	final Node ROOT = new Node(' ', null);

	public Tries() {
		super();
	}

	public Tries(String word) {
		this();
		add(word);
	}

	public void add(final String word) {
		if (word != null && word.length() > 0) {
			StringBuffer sb = new StringBuffer();
			char[] charArray = word.toCharArray();

			for (char c : charArray) {
				if (c == ' ') {
					addWord(sb);
					sb = new StringBuffer();
				} else {
					sb.append(c);
				}
				index++;
			}
			addWord(sb);
		}
	}

	private void addWord(StringBuffer sb) {
		char[] charArray2 = sb.toString().toCharArray();
		Node n = ROOT;
		int idx = index - charArray2.length + 1;
		for (char ac : charArray2) {
			n = n.add(ac, idx++);
		}
		n.isLast = true;
	}

	int index = 0;

	private Node search(final String prefix) {
		char[] charArray = prefix.toCharArray();
		Node n = ROOT;
		for (char c : charArray) {
			n = n.search(c);
			if (n == null)
				break;
		}
		if (n.getPrefix().equals(prefix)) {
			return n;
		} else {
			return null;
		}
	}

	public Set<String> getWords(final String prefix) {
		Node search = search(prefix);
		if (search != null) {
			final DoublyLinkedSet<String> allWords = new DoublyLinkedSet<String>();
			final DoublyLinkedStack<Node> alltails = new DoublyLinkedStack<Tries.Node>();
			alltails.addAll(search.nodes);
			Node poll = null;
			while ((poll = alltails.pop()) != null) {
				if (poll.isLast) {
					allWords.add(poll.getPrefix());
				}
				alltails.addAll(poll.nodes);
			}
			return allWords;
		} else {
			return null;
		}
	}

	public int[] searchIndexes(final String prefix) {
		Node search = search(prefix);
		if (search != null) {
			int[] array = search.indexes.toArray();
			int[] arr = new int[array.length];
			int i = 0;
			int s = prefix.length();
			for (int j : array) {
				arr[i++] = j - s;
			}
			return arr;
		} else {
			return new int[] {};
		}
	}

//	public static void main(String[] args) {
//		Tries tries = new Tries(
//				"India being an English speaking country has one of the poorest living standards than say Australia or US which are "
//						+ "first world countries with high living standards . India also has more English speaking population who are ready to undergo harsh "
//						+ "treatment at the hands of any one who can send them to these countries for cheap dollar amount while the actual job might reward a "
//						+ "person with higher salary in those native countries.");
//
//		System.out.println(net.sourceforge.sizeof.SizeOf.sizeOf(tries));
//	}

}
