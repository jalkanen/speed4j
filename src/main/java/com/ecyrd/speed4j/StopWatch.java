/*
   Copyright 2011 Janne Jalkanen

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ecyrd.speed4j;

import java.io.Serializable;

/**
 *  Creates a simple StopWatch with nanosecond precision (though not necessarily accuracy).
 *  <p>
 *  A "tag" is an unique grouping identifier. A "message" can be anything you like; it just
 *  travels with the StopWatch and is output with the toString method.
 *  <p>
 *  Tags must not contain whitespace, forward slash or commas. All other characters are allowed.
 *  <p>
 *  Most of the StopWatch methods return a reference to itself for easy chaining.
 */
public class StopWatch implements Serializable
{
    private static final String DEFAULT_TAG = "[default]";

    private static final long serialVersionUID = 5154481161113185022L;

    private long   m_creation;
    private long   m_startNanos;
    private long   m_stopNanos;
    private int   m_count;
    private String m_tag;
    private String m_message;

    private static final long NANOS_IN_SECOND = 1000*1000*1000;

    public StopWatch()
    {
        this( null, null);
    }

    public StopWatch( String tag )
    {
        this( tag, null );
    }

    public StopWatch( String tag, String message )
    {
        m_tag = tag;
        m_message = message;
        m_creation = System.currentTimeMillis();
        m_count = 1;
        start();
    }

    /**
     *  Starts a StopWatch which has been previously stopped.  If the StopWatch was already running,
     *  this method will reset it.
     *
     *  @return This StopWatch.
     */
    public StopWatch start()
    {
        m_startNanos = System.nanoTime();

        return this;
    }

    /**
     *  The internal stop() method, which can be overridden by subclasses to provide additional
     *  functionality at stop().  Don't forget to call super.stop() in your subclass, or else
     *  the clock will not be stopped.
     */
    protected void internalStop()
    {
        m_stopNanos = System.nanoTime();
    }

    /**
     *  Stops the StopWatch.
     *
     *  @return This StopWatch instance.
     */
    public StopWatch stop()
    {
        internalStop();

        return this;
    }

    /**
     *
     *
     * @param count
     * @return
     */
    public StopWatch stop(int count)
    {
        m_count = count;
        stop();

        return this;
    }

    /**
     *  Stops the StopWatch and assigns the given tag to it.
     *
     *  @param tag The tag to assign.
     *  @return This StopWatch.
     */
    public StopWatch stop( String tag )
    {
        m_tag = tag;
        stop();

        return this;
    }

    public StopWatch stop( String tag , int count)
    {
        m_tag = tag;
        stop(count);

        return this;
    }

    /**
     *  Stops the StopWatch, assigns the tag and a free-form message.
     *
     *  @param tag The tag to assign.
     *  @param message A free-form message that associates with this particular StopWatch.
     *  @return This StopWatch.
     */
    public StopWatch stop( String tag, String message )
    {
        m_tag = tag;
        m_message = message;
        stop();

        return this;
    }

    public StopWatch stop( String tag, String message, int count )
    {
        m_tag = tag;
        m_message = message;
        stop(count);

        return this;
    }

    /**
     *  Stops and starts the StopWatch, essentially resetting it.
     *
     *  @return This StopWatch.
     */
    public StopWatch lap()
    {
        stop();
        start();

        return this;
    }

    /**
     *  Returns the message associated with this StopWatch.
     *
     *  @return The message, or null, if no message has been associated.
     */
    public String getMessage()
    {
        return m_message;
    }

    public void setMessage(String msg)
    {
        m_message = msg;
    }
    
    /**
     *  Returns the tag (grouping) for this StopWatch.
     *
     *  @return The tag, or null, if no tag has yet been assigned.
     */
    public String getTag()
    {
        return m_tag;
    }

    /**
     *  Set the tag (grouping) for this StopWatch.
     *
     *  @param tag A valid string tag.
     */
    public void setTag(String tag)
    {
        m_tag = tag;
    }

    /**
     *  Returns the elapsed time in nanoseconds.
     *
     *  @return
     */
    private long getTimeNanos()
    {
        if( m_stopNanos != 0 )
            return m_stopNanos - m_startNanos;

        return System.nanoTime() - m_startNanos;
    }

    /**
     *  Returns the elapsed time in microseconds.
     *
     *  @return The elapsed time in microseconds.
     */
    public long getTimeMicros()
    {
        return getTimeNanos() / 1000;
    }

    /**
     *  Returns the elapsed time in milliseconds.  This method is a convenience method
     *  for those upgrading from Perf4J.
     *
     *  @return Elapsed time in milliseconds.
     */
    public long getElapsedTime()
    {
        return getTimeNanos() / 1000000;
    }

    /**
     *  Returns the moment in time at which this StopWatch was created (milliseconds since EPOCH).
     *
     *  @return Start time.
     */
    public long getCreationTime()
    {
        return m_creation;
    }

    /**
     * Returns the number of items that were measured in one StopWatch
     *
     * @return count
     */
    public long getCount()
    {
        return m_count;
    }

    /**
     *  Returns a human-readable string.  This is a slowish op, so don't call unnecessarily.
     *  Do NOT rely this in being any particular format.
     */
    // Uses concat() to achieve some minor speed upgrades.
    public String toString()
    {
        return (m_tag != null ? m_tag : DEFAULT_TAG).concat(": ").concat(getReadableTime()).concat( (m_message != null ? " "+m_message : "") );
    }

    /**
     *  Returns a human readable string which also calculates the speed of a single
     *  operation.  Do NOT rely on this being in any particular format. For example:
     *
     *  <pre>
     *    StopWatch sw = ...
     *    for( int i = 0; i < 1000; i++ )
     *    {
     *       // Do something
     *    }
     *    sw.stop("test");
     *    System.out.println( sw.toString(1000) );
     *  </pre>
     *  This might print out something like:
     *  <pre>
     *    test: 14520 ms (68 iterations/second)
     *  </pre>
     *
     *  @param iterations
     *  @return
     */
    public String toString( int iterations )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( m_tag != null ? m_tag : DEFAULT_TAG);
        sb.append( ": " );
        sb.append( getReadableTime() );
        if( m_message != null ) sb.append(" "+m_message);
        sb.append( " ("+iterations * NANOS_IN_SECOND / getTimeNanos()+" iterations/second)");

        return sb.toString();
    }

    /**
     *  Returns a the time in something that is human-readable.
     *
     *  @return A human-readable time string.
     */
    private String getReadableTime()
    {
        long ns = getTimeNanos();

        if( ns < 50L * 1000 )
            return ns + " ns";

        if( ns < 50L * 1000 * 1000 )
            return (ns/1000)+" us";

        if( ns < 50L * 1000 * 1000 * 1000 )
            return (ns/(1000*1000))+" ms";

        return ns/NANOS_IN_SECOND + " s";
    }

    /**
     *  Returns a cloned, freezed copy of the StopWatch.  The returned StopWatch is
     *  automatically stopped.
     *
     *  @return
     */
    // TODO: Should probably return a FrozenStopWatch
    public StopWatch freeze()
    {
        StopWatch sw = new StopWatch( m_tag, m_message);
        sw.m_startNanos = m_startNanos;
        sw.m_stopNanos = m_stopNanos != 0 ? m_stopNanos : System.nanoTime();
        sw.m_creation = m_creation;
        sw.m_count = m_count;
        return sw;
    }
}
