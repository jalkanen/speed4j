package com.ecyrd.zoom4j;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class StopWatchFactory
{
    // TODO: Add configuration
    private static Log m_log = new PeriodicalLog(LoggerFactory.getLogger(StopWatchFactory.class));
    
    public static StopWatch getStopWatch()
    {
        return getStopWatch(null,null);
    }
    
    public static StopWatch getStopWatch( String tag )
    {
        return getStopWatch( tag,null );
    }
    
    public static StopWatch getStopWatch( String tag, String message )
    {
        return new LoggingStopWatch( m_log, tag, message );
    }
    
    public static void shutdown()
    {
        m_log.shutdown();
        m_log = null;
    }
    
    @Test
    public void test() throws InterruptedException
    {
        for( int i = 0; i < 12200; i++ )
        {
            StopWatch sw = getStopWatch("foo");
            
            Thread.sleep(10+ (long)(Math.random() * 10));
            
            sw.stop("iteration:success");
        }
    }
}
