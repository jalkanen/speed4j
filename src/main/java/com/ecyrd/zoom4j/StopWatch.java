package com.ecyrd.zoom4j;

import java.io.Serializable;

/**
 *  Creates a simple StopWatch with nanosecond precision (though not necessarily accuracy).
 *  <p>
 *  A "tag" is an unique grouping identifier.
 */
public class StopWatch implements Serializable
{
    private static final long serialVersionUID = 5154481161113185022L;

    private long   m_startNanos;
    private long   m_stopNanos;
    private String m_tag = "?";
    private String m_message;
        
    protected StopWatch( String tag, String message )
    {
        m_tag = tag;
        m_message = message;
        start();
    }
    
    public StopWatch start()
    {
        m_startNanos = System.nanoTime();
        
        return this;
    }
    
    protected void internalStop()
    {
        m_stopNanos = System.nanoTime();
    }
    
    public StopWatch stop()
    {
        internalStop();
        
        return this;
    }
    
    public StopWatch stop( String tag )
    {
        m_tag = tag;
        stop();
        
        return this;
    }
    
    public StopWatch stop( String tag, String message )
    {
        m_tag = tag;
        m_message = message;
        stop();
        
        return this;
    }
    
    public StopWatch lap()
    {
        stop();
        start();
        
        return this;
    }
    
    public String getMessage()
    {
        return m_message;
    }
    
    public String getTag()
    {
        return m_tag;
    }
    
    /**
     *  Returns the elapsed time in nanoseconds.
     *  
     *  @return
     */
    public long getTimeNanos()
    {
        if( m_stopNanos != 0 )
            return m_stopNanos - m_startNanos;
        
        return System.nanoTime() - m_startNanos;
    }
    
    /**
     *  Returns a human-readable string.  This is a slowish op, so don't call unnecessarily.
     */
    public String toString()
    {
        return m_tag+": "+getTimeNanos()+" ns" + (m_message != null ? m_message : "");
    }

    /**
     *  Returns a cloned, freezed copy of the StopWatch.  The returned StopWatch is
     *  automatically stopped.
     *  
     *  @return
     */
    // TODO: Should probably return a FrozenStopWatch
    protected StopWatch freeze()
    {
        StopWatch sw = new StopWatch( m_tag, m_message );
        sw.m_startNanos = m_startNanos;
        sw.m_stopNanos = m_stopNanos != 0 ? m_stopNanos : System.nanoTime();
        
        return sw;
    }
}
