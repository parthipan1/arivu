# High performance resourcepool
Pure java solution for age old resource pool [problem](https://en.wikipedia.org/wiki/Object_pool_pattern). It uses new design [patterns](https://en.wikipedia.org/wiki/Monitor_(synchronization)) for multi threading to manage resoures . It gives the best performance in multi threaded environment. Bench mark Junit test are available and can be checkout and tested for all resource pools. Beter than Apache [pool](https://commons.apache.org/proper/commons-pool/index.html) and other libraties in terms of usage and performance.

**How to use in your project:**
* 1. Checkout the project.
* 2. RUn Gradle clean build on project.
* 3. Jar file will generated in the build folder.

**Salient feature :**
* 1. Pure java solution for resource pool [problem](https://en.wikipedia.org/wiki/Object_pool_pattern).
* 2. Does not use [hashcode](http://www.javaranch.com/journal/2002/10/equalhash.html) based collection libraries , instead used inbuilt circular linked list to maintain resource pool of any object ,  which provides highly reliable fast read/write access to objects.
* 3. Any type of Objects can be used in this pool library. If AutoClosable objects are used then no need to explicitly release the object , other wise closing the object is fine.
* 4. Multi thread safe pool library.
* 5. Very high performance library, can be used on large scale highly scalable environments. 
* 6. All pool Implementations support maxPoolSize property except for NoPool and ThreadlocalPool( uses thread count ).
* 7. Very light weight non evasive library which can be integrated into all existing frame works.
* 8. Uses latest concurrency [paradigm](https://en.wikipedia.org/wiki/Java_concurrency) to solve thread contention issue, uses a new design [patterns](https://en.wikipedia.org/wiki/Monitor_(synchronization)) for multithreading which give extra edge in performance.
* 9. Used by other frameworks like [datasource](https://github.com/parthipan1/datasource) to manage their internal resources.


Please feel free to use it and report any bugs for further improvements. 
My Site - [link](https://myblogbookin.wordpress.com/2016/08/04/new-resource-pool-library-for-java/)
