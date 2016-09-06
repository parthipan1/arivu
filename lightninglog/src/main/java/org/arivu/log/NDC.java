package org.arivu.log;

import java.util.Map;

import org.arivu.datastructure.DoublyLinkedStack;
import org.arivu.datastructure.Threadlocal;

public class NDC { 
	static final Threadlocal<DoublyLinkedStack<String>> tl = new Threadlocal<DoublyLinkedStack<String>>(new Threadlocal.Factory<DoublyLinkedStack<String>>() {

		@Override
		public DoublyLinkedStack<String> create(Map<String, Object> arg0) {
			return new DoublyLinkedStack<String>();
		}
	}, -1);
	
	  public static int size() { 
		  return tl.get(null).size();
	  } 
	 
	  public static void push(String val) { 
		  tl.get(null).push(val);
	  } 
	 
	  public static String pop() { 
		  return tl.get(null).pop();
	  } 
	 
	  public static void clear(){
		  tl.get(null).clear();
	  }
	}
