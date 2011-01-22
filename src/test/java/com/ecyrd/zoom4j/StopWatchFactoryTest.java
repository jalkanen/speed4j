package com.ecyrd.zoom4j;

import org.junit.Test;


public class StopWatchFactoryTest
{
    
    @Test
    public void test() throws InterruptedException
    {
        StopWatchFactory swf = StopWatchFactory.getDefault();
        
        int iterations = 120;
        
        for( int i = 0; i < iterations; i++ )
        {
            StopWatch sw = swf.getStopWatch("foo");
            
            Thread.sleep(10+ (long)(Math.random() * 10));
            
            sw.stop("iteration:success");
        }
       
    }

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

        System.out.println(sw);
        System.out.println(sw.toString(iterations));
    }

    @Test
    public void testSpeed() throws InterruptedException
    {
        StopWatchFactory swf = StopWatchFactory.getDefault();
        
        long time = System.currentTimeMillis();
        int iterations = 1200000;
        
        for( int i = 0; i < iterations; i++ )
        {
            StopWatch sw = swf.getStopWatch("foo");
                        
            sw.stop("iteration:success");
        }
        
        time = System.currentTimeMillis() - time;
        
        System.out.println("Iterations: "+iterations+", total time "+time+" ms, is "+(iterations*1000/time)+" iterations per second");
    }
    
    @Test
    public void testSlf4jLog() throws InterruptedException
    {
        Slf4jLog log = new Slf4jLog();
        log.setSlf4Logname("foo");
        StopWatchFactory swf = StopWatchFactory.getInstance(log);
        
        int iterations = 100;
        
        for( int i = 0; i < iterations; i++ )
        {
            StopWatch sw = swf.getStopWatch("foo");

            Thread.sleep(10+ (long)(Math.random() * 10));

            sw.stop("iteration:success");
        }
        
    }

    @Test
    public void testPeriodicalLog() throws InterruptedException
    {
        PeriodicalLog log = new PeriodicalLog();
        log.setSlf4Logname("foo");
        log.setPeriod("5");
        
        StopWatchFactory swf = StopWatchFactory.getInstance(log);
        
        int iterations = 1000;
        
        for( int i = 0; i < iterations; i++ )
        {
            StopWatch sw = swf.getStopWatch("foo");

            long waitPeriod = (long)(Math.random() * 10);
            
            Thread.sleep(10+waitPeriod);

            sw.stop("iteration:"+waitPeriod);
        }
        
    }

}
