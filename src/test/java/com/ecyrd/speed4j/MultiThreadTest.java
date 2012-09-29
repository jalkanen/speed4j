package com.ecyrd.speed4j;

import java.lang.management.ManagementFactory;

import javax.management.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ecyrd.speed4j.log.PeriodicalLog;

public class MultiThreadTest
{
    StopWatchFactory swf;
    PeriodicalLog pl;
    @Before
    public void setUp()
    {
        pl = new PeriodicalLog();
        
        pl.setName( "multithread" );
        pl.setPeriod( 5 );
        pl.setSlf4jLogname( "multithread" );
        pl.setJmx( "Count" );
        swf = new StopWatchFactory( pl );
    }
    
    @After
    public void cleanUp()
    {
        pl.shutdown();
    }
    
    // This test fails if there's a ConcurrentModificationException or similar being thrown.
    @Test
    public void testMultiThread() throws MalformedObjectNameException, NullPointerException, IntrospectionException, InstanceNotFoundException, ReflectionException
    {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName("Speed4J: name="+pl.getName());
        MBeanInfo info = mbeanServer.getMBeanInfo( on );
        MBeanAttributeInfo[] attrs = info.getAttributes();

        int numThreads = 100;
        Thread[] threads = new Thread[numThreads];
        
        for( int i = 0; i < numThreads; i++ )
        {
            threads[i] = new Thread( new TestRunnable() );
            threads[i].start();
        }
        
        try
        {
            Thread.sleep(30*1000L);
        }
        catch( InterruptedException e ) {}
        
        // This should be okay.
        for( Thread t : threads ) t.stop();
    }
    
    private class TestRunnable implements Runnable
    {

        public void run()
        {
            while(true)
            {
                StopWatch sw = swf.getStopWatch();
                
                double d = 0.0;
                for( int i = 0; i < 1000; i++ )
                {
                    d += Math.random();
                }
//                try
//                {
//                    Thread.sleep(1000);
//                }
//                catch( InterruptedException e )
//                {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
                sw.stop("Count");
            }
        }
        
    }
}
