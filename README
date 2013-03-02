
This is Speed4j, a very simple (but fast) Java performance analysis library. It is
designed using Perf4j as a model, but hopefully avoiding the pitfalls inherent
in Perf4j's design.  Also, Perf4j does not seem to be seeing a lot of development
these days...

Speed4j has a dependency on SLF4J (see http://slf4j.org), which it uses to log
its workings, but no other dependencies.

INSTALLING
==========

Speed4j is available in Maven Central.  You can download the artifacts directly
from

http://repo2.maven.org/maven2/com/ecyrd/speed4j/speed4j/

Or, you can put this into your pom.xml:

<dependency>
    <groupId>com.ecyrd.speed4j</groupId>
    <artifactId>speed4j</artifactId>
    <version>0.12</version>
</dependency>

Or, if you like Apache Ivy more, use this:

<dependency org="com.ecyrd.speed4j" name="speed4j" rev="0.12"/>



SIMPLEST POSSIBLE USAGE
=======================

public void myBusyMethod()
{
	StopWatch sw = new StopWatch();

	// Do the busy thing

	sw.stop();
	System.out.println(sw);
}

This would simply print out how fast the block was.  Very useful for quick performance
testing.  Also, you could do

GETTING ITERATIONS TOO
======================

public void myBusyMethod()
{
	StopWatch sw = new StopWatch();

	int iterations = 1000;

	for( int i = 0; i < iterations; i++ ) {
		// Do the busy thing
	}

	sw.stop();

	System.out.println(sw.toString(iterations));
}

This would print out how fast the block was, *and* how many times the iterated
block would execute per second.

However, speed4j is designed to be a part of your application.  So we could
also do the following:

A COMPLEX EXAMPLE
=================

public void myBusyMethod2()
{
	StopWatch sw = myStopWatchFactory.getStopWatch();

	try
	{
		// Do the busy thing

		// Notice that sw.stop() automatically logs if the Factory is configured so
		sw.stop("busyThing:success");
	}
	finally
	{
		sw.stop("busything:failure");
	}
}

Wait, where did the "myStopWatchFactory" come from?  Well, you initialized it somewhere
in your app or class with

StopWatchFactory myStopWatchFactory = StopWatchFactory.getInstance("loggingFactory");

This is where it gets interesting.  Speed4j will read the configuration for the factory
named in the call (in this case, "loggingFactory") from a file called "speed4j.properties"
from your class path.  Let's see how a sample file would look like:

SPEED4J.PROPERTIES
=================

   speed4j.loggingFactory=com.ecyrd.speed4j.log.Slf4jLog
   speed4j.loggingFactory.slf4jLogname=com.example.mylog

So, this defines a Log called "loggingFactory" which uses the "com.ecyrd.speed4j.log.Slf4jLog"
instance to do the logging.  This particular class would connect to the given SLF4J logger
and log using the info() method to it.  So depending your setup, this would then go to the
log4j or console or wherever.


PERIODICAL LOGGING
==================

If you are tired of manually looking at your log to figure out how fast something is, then
PeriodicalLog is your friend.

speed4j.loggingFactory=com.ecyrd.speed4j.log.PeriodicalLog
speed4j.loggingFactory.period=60
speed4j.loggingFactory.jmx=busything:success,busything:failure
speed4j.loggingFactory.slf4jLogname=com.example.myperiodicalllog

OK, lots of goodies happening here.  First you can set the period during which the stats
are collected, in this case 60 seconds.

Second, if the "jmx" attribute is set for a PeriodicalLog, it will expose a JMX management
bean which will list the average, standard deviation, count, min and max values for the given
tags.

Finally, it will also output a collected string to the given SLF4J logger, resulting into
something that looks like this:

19400 [Thread-3] INFO foo - Statistics from Tue Apr 19 23:11:15 EEST 2011 to Tue Apr 19 23:11:20 EEST 2011
19400 [Thread-3] INFO foo - Tag                                       Avg(ms)      Min      Max  Std Dev     95th   Count
19401 [Thread-3] INFO foo - iteration:9                                 19.18    19.07    19.88     0.14    19.60      28
19401 [Thread-3] INFO foo - iteration:8                                 18.17    18.09    18.33     0.06    18.32      26
19402 [Thread-3] INFO foo - iteration:7                                 17.19    17.07    17.48     0.08    17.43      26
19402 [Thread-3] INFO foo - iteration:6                                 16.17    16.07    16.26     0.05    16.25      23
19402 [Thread-3] INFO foo - iteration:5                                 15.18    15.08    15.31     0.06    15.29      27
19403 [Thread-3] INFO foo - iteration:4                                 14.16    14.08    14.28     0.06    14.28      21
19403 [Thread-3] INFO foo - iteration:3                                 13.17    13.08    13.26     0.05    13.26      21
19404 [Thread-3] INFO foo - iteration:2                                 12.17    12.08    12.25     0.05    12.25      32
19404 [Thread-3] INFO foo - iteration:1                                 11.16    11.07    11.30     0.06    11.30      16
19405 [Thread-3] INFO foo - iteration:0                                 10.16    10.08    10.27     0.05    10.26      24

(The 95th means the 95th percentile, i.e. 95% of all calls stayed under this limit.  This is very useful
for figuring out which methods have wildy varying performance.)

PeriodicalLog requires Java 6.


PERCENTILES
===========

Finding percentile information is very useful.  By default, speed4j will output
the 95th percentile (i.e. 5% of log calls took more than this), but it is fully
configurable.  To get logging data for 95th, 99th and 99.9th percentiles, use
a statement like this:

speed4j.loggingFactory.percentiles=95,99,99.9

The percentiles will be visible in JMX under <tag>/95, <tag>/99 and <tag>/99.9
respectively.


SLOW LOG
========

So now you know that you have 99.9% of all requests resolving in less than 100ms.
But what about those go over?  It would be useful to get some sort of logging
about them, so Speed4j supports automatic logging of those events.

You can configure slow logging using PeriodicalLog:

speed4j.loggingFactory.slowLogname = myloggername
speed4j.loggingFactory.slowLogPercentile = 99.9

This will result in all StopWatches which belong to the 0.1% that do not complete
in time to be logged to "myloggername" SLF4J log.

There are some caveats though:
1) Turning slow logging on may cause some slowdown of your app, especially if
   there's a ton of requests flying over the limit - which may happen if the
   variance of your stopwatches is very large.
2) Percentiles are only calculated when the period time elapses - so if you
   are logging every 60 seconds, that percentile data will be valid for the
   *next* 60 seconds.  So the percentiles will be lagging one period behind.
3) Slow logging will consume some extra memory which is currently not freed.
   So using a lot of tags in your app may cause a bit of a memory leak. However,
   we're talking kilobytes here.
   
   If you stop measuring slow log by either turning it off from JMX or setting
   the slowLog name to null via PeriodicalLog.setSlowLogname(), the memory
   is freed (and consequently, relogging will start when you turn it back on).

The slow log thresholds can be changed using JMX as well at runtime (but the
logger name cannot).

The log format is

  2012-09-30T21:53:30.703+0200 99.9 "Count" "From thread Thread-54" 518.216

* The creation time of the StopWatch as ISO-8601 -formatted date
* The used percentile threshold
* Tag
* Message
* Elapsed time in milliseconds


PROGRAMMATIC CONFIGURATION
==========================

So property files ain't good for you?  You can also try the following:

public void myTest()
{
	PeriodicalLog myLog = new PeriodicalLog();
	myLog.setName("loggingFactory");
	myLog.setPeriod("60");
	myLog.setJmx("busything:success,busything:failure");
	myLog.setSlf4jLogname("com.example.myperiodicallog");

	StopWatchFactory swf = StopWatchFactory.getInstance(myLog);

	StopWatch sw = swf.getStopWatch();
	...
}

The properties are directly accessible also via the appropriate setters.  In fact,
all the speed4j configuration module does is that it just simply calls the appropriate
setter module - so if a property is called "speed4j.myLog.period=50" it will call
setPeriod(50) on myLog.

TRICKS AND TIPS
===============

Q: I want to see just the JMX data, not the logging.

A: Configure the PeriodicalLog's mode with

   log.setMode( PeriodicalLog.Mode.JMX_ONLY );

   or from a property file

   mylog.mode = JMX_ONLY


Q: Ok, so how about not showing JMX statistics?

A: Just use mode "LOG_ONLY".  By default, Speed4j is in "ALL" mode, but you
   can also set it to "QUIET" if you don't want any logging to occur.
   

Q: It seems that there are a lot of StopWatches in memory! Memory leak! Waaaa!

A: The StopWatches are collected in a separate Thread.  In a high-CPU usage scenario
   it's possible that the Thread is starved, and it just can't keep up.  For such
   cases the StopWatch queue has a maximum size which you can set with e.g.

   speed4j.loggingFactory.maxQueueSize = 10000 # It can now hold up to 10,000 items.

   The default size is 300,000 items, which should be plenty.

   You can monitor the queue size with JMX, as well as the number of dropped
   StopWatches (when the queue flows over).


Q: I want to use speed4j, but the library I use already uses it.

A: If the library in question ships with its own speed4j.properties, then
   you can set up your own speed4j.properties by passing a system property. E.g.

   java -Dspeed4j.properties=myapplication_speed4j.properties

   Make sure that you also configure the library's settings in there, or
   otherwise they won't log.

   The property file will be searched from your classpath, including /WEB-INF/

   Another way to do this is to use StopWatchFactory.getInstance( filename, factoryName ).
   You can specify a filename which is searched for in your classpath. Any
   factories found in that file will be added to the global list of factories,
   so be careful.

Q: I need more/less space for tag names in PeriodicalLog's output!

A: Set the desired width e.g to 80 characters with

   speed4j.<factoryName>.tagFieldWidth = 80


LICENSE INFORMATION
===================

Speed4j is distributed under the Apache Software License, version 2.0 (see LICENSE).

Speed4j contains code (C) The Apache Software Foundation, 2003-2004.


SOURCE CODE AND SUPPORT
=======================

Speed4j is available on GitHub, http://github.com/jalkanen/speed4j

There is a support email group at http://groups.google.com/group/speed4j-users
