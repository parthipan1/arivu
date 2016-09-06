# datastructure
Collection of datastructures not available in default java framework.

Here is a collection of special data structures which are not available in standard java library. Collection includes structures like Tries , DoublyLinkedList , DoublyLinkedSet ,  DoublyLinkedStack/Queue , LruCache etc. Also support for primitive datatypes.

**Few points about the data structures**

* Thread safe data structures.
* DoublyLinkedList can be used as circular buffer , perfectly suited for low latency , high thru put environments.
* Works with all existing frameworks including java standard collection framework.
* LinkedList  good for sequential access not for random access.
* Tries are very good in prefix search and indexing String searches. Other use cases can be added later.
* Pure java solution with no external dependencies.
* Other data structures like high performance Maps are on the way.
* Junit test cases exists for all use cases.

**Limitations:**

* Supports only sequential access, performance may deteriorate on random access large data set.
