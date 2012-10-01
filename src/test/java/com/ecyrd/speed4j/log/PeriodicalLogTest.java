package com.ecyrd.speed4j.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import javax.management.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ecyrd.speed4j.StopWatchFactory;
import com.ecyrd.speed4j.log.PeriodicalLog.Mode;


public class PeriodicalLogTest
{
    StopWatchFactory swf;
    PeriodicalLog pl;
    
    @Before
    public void setUp()
    {
        pl = new PeriodicalLog();
        
        pl.setName( "PeriodicalLogTest" );
        pl.setPeriod( 5 );
        pl.setSlf4jLogname( "PeriodicalLogTest" );

        swf = new StopWatchFactory( pl );
    }
    
    @After
    public void cleanUp() throws MalformedObjectNameException, NullPointerException
    {
        pl.shutdown();

        // Check that JMX has been removed.
        
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        ObjectName on = new ObjectName("Speed4J: name="+pl.getName());

        assertFalse( "still registered!", mbeanServer.isRegistered(on) );
    }
    
    /**
     *  Test that JMX objects can be reregistered properly.
     */
    @Test
    public void testJMX() throws MalformedObjectNameException, NullPointerException, IntrospectionException, InstanceNotFoundException, ReflectionException
    {
        pl.setSlf4jLogname("");
        pl.setPercentiles( "99,99.9" );
        
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        ObjectName on = new ObjectName("Speed4J:name="+pl.getName());
        
        assertTrue( "jmx bean '"+on+"' is not registered!", mbeanServer.isRegistered(on) );
        
        MBeanInfo info = mbeanServer.getMBeanInfo( on );
        
        MBeanAttributeInfo[] attrs = info.getAttributes();
        
        assertEquals( "Should have just the defaults for now", 5, attrs.length );
        
        //  Now, let's add some new stuff.
        
        pl.setJmx("test");
        
        MBeanInfo info2 = mbeanServer.getMBeanInfo( on );        
        MBeanAttributeInfo[] attrs2 = info2.getAttributes();
        assertEquals( "Should have now test added", 12, attrs2.length );
        
        // Turn to log only mode and see if JMX disappears
        pl.setMode( Mode.LOG_ONLY );
        
        try
        {
            @SuppressWarnings("unused")
            MBeanInfo info3 = mbeanServer.getMBeanInfo( on );  
            fail("MBean did not disappear");
        }
        catch( InstanceNotFoundException e ) {} // Expected
    }
    
    
}
