package com.ecyrd.zoom4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StopWatchTest
{
    @Test
    public void testToStringIterations() throws InterruptedException
    {
        StopWatchFactory swf = StopWatchFactory.getDefault();
        
        int iterations = 100;

        StopWatch sw = swf.getStopWatch("foo");
                
        for( int i = 0; i < iterations; i++ )
        {
            Thread.sleep(10+ (long)(Math.random() * 10));
        }

        sw.stop("ok");

        assertEquals("ok", sw.getTag());
        
        assertTrue( sw.getTimeNanos() > iterations*10*1000000 ); // No way this could be faster
        
        String s = sw.toString(iterations);

        assertTrue( s.contains("iterations/second"));
    }
    
    @Test
    public void testSpeed() throws InterruptedException
    {
        StopWatchFactory swf = StopWatchFactory.getDefault();
        
        long time = System.currentTimeMillis();
        int iterations = 1200000;

        StopWatch total = swf.getStopWatch();
        
        for( int i = 0; i < iterations; i++ )
        {
            StopWatch sw = swf.getStopWatch("foo");

            // Do nothing
            
            sw.stop("iteration:success");
        }
        
        total.stop("total");
        time = System.currentTimeMillis() - time;
        
        // Output perf data
        System.out.println("Iterations: "+iterations+", total time "+time+" ms, is "+(iterations*1000/time)+" iterations per second");

        assertTrue( "StopWatch and system count differ by more than 1 ms", Math.abs(time - total.getTimeNanos()/1e6) < 1 );
    }

}
