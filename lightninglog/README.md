# lightninglog
Java Garbage free logger. Also known as LightingLog (Fast as a lighting strike, hot as a hell, harbinger for deluge of logs to come).

Pure java solution for fast logging. Has dependency on slf4j. Works with existing frameworks . 
set the json file and copy the jar file to classpath .  

sample lightninglog.json =>
```java
{
	"appenders":["zip"],
	"loggers":{
		"root":"debug",
		"com.xxx":"debug"
	},
	"buffer":{
		"batch":100,
		"ring":300
	},
	"log":{
		"showDateTime":true,
		"showThreadName":true,
		"showName":true,
		"showShortName":true,
		"file":"logs//xxxx.log",
		"fileSize":5242880000,
		"dateTimeFormat":"yyyy-MM-dd HH:mm:ss:SSS Z",
		"fileDateTimeExt":"yyyy-MM-dd'T'HH:mm:ss:SSS'Z'"
	}
}
```
**Salient features:**
  * 1.High performance logging framework , works wells in multi threaded environment.
  * 2.Lock free algorithm(CAS) to solve thread contention by using circular buffers for logs with very low latency.
  * 3.Every thing happens concurrently on main thread , no extra treads is created.
  * 4.Used Ring buffer to write logs to system it garantes o(1) , because of a custom algorithm implemetation in Log processing.
  * 5.Overall performance improvement due to minimal log push on main threads.
  * 6.Every logs generated will be captured nothing will be missed for normal operations.
  * 7.Configuration based on json config file (Sample is attached above).
  * 8.Performance comparable with Log4j2 and Logback async Appenders.
  * 9.Excellent jmx support for dynamic scaling of loggers.
  * 10. Make sure you have following setting enabled on JVM (Optional) ```-Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 ```
  * 11 Garbage free algorithm for processing logs with minimal impact on garbage collector.
  * 12 MDC and NDC are supported , But Layout is not supported but will be added later. 

**Limitations:**
  * 1.Only console and file appenders(writers) available. Other writers like database,sockets etc are on the way.
  * 2.System abrupt interruptions(like power failure, terminations errors) may result in log losses.
  * 3.Filters and Markers are not currently supported , but will be added later.
