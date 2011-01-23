package com.ecyrd.zoom4j.log;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.management.*;

import com.ecyrd.zoom4j.ConfigurationException;
import com.ecyrd.zoom4j.StopWatch;

/**
 *  A Periodical log which can also expose its attributes via JMX.
 *  <p>
 *  The JMX name is based on the name of the Log.  So if you don't set
 *  it via {@link #setName(String)}, you'll end up something that Zoom4J
 *  picks up on its own.  Normally, if you use the property file to
 *  configure Zoom4J, this gets automatically assigned for you.
 */
public class PeriodicalLog extends Slf4jLog implements DynamicMBean
{
    private static final String ATTR_POSTFIX_MAX = "/max";
    private static final String ATTR_POSTFIX_MIN = "/min";
    private static final String ATTR_POSTFIX_STDDEV = "/stddev";
    private static final String ATTR_POSTFIX_AVG = "/avg (ms)";
    private static final String ATTR_POSTFIX_COUNT = "/count";
    
    private Queue<StopWatch> m_queue              = new ConcurrentLinkedQueue<StopWatch>();
    private Thread           m_collectorThread;
    private boolean          m_running            = true;
    private int              m_periodSeconds      = 30;
    private MBeanServer      m_mbeanServer        = null;
    private String[]         m_jmxAttributes      = null;
    private MBeanInfo        m_beanInfo;
    private HashMap<String,CollectedStatistics> m_statistics;
    
    /**
     *  Creates an instance of PeriodicalLog.
     */
    public PeriodicalLog()
    {
        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run()
            {
                shutdown();
            }
        });
    }

    @Override
    public void log(StopWatch sw)
    {
        //
        //  Start the collector lazily.
        //
        if( m_collectorThread == null )
        {
            m_collectorThread = new CollectorThread();

            m_collectorThread.start();        
        }
        
        m_queue.add( sw.freeze() );
    }
    
    /**
     *  Set the tags which are shown via JMX.
     *  
     *  @param value A comma-separated list of tags which are shown via JMX.
     */
    public void setJmx( String value )
    {
        m_jmxAttributes = value.split(",");
        
        m_mbeanServer = ManagementFactory.getPlatformMBeanServer();
            
        try
        {
            buildMBeanInfo();
            
            //
            //  Remove and reinstall from the MBean registry if it already exists.
            //
            if( m_mbeanServer.isRegistered(getJMXName()) )
                m_mbeanServer.unregisterMBean(getJMXName());
            
            m_mbeanServer.registerMBean( this, getJMXName() );
        }
        catch (InstanceAlreadyExistsException e)
        {
            // OK
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }
    
    /**
     *  Shuts down the collector thread and removes the JMX bean
     *  if it is registered.
     */
    @Override
    public void shutdown()
    {
        m_running = false;
        if( m_collectorThread != null ) m_collectorThread.interrupt();

        try
        {
            //
            //  Remove MBean
            //
            if( m_mbeanServer.isRegistered(getJMXName()) )
                m_mbeanServer.unregisterMBean(getJMXName());
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     *  The name under which this Log should be exposed as a JMX bean.
     *  
     *  @return A ready-to-use ObjectName.
     *  
     *  @throws MalformedObjectNameException If your name is faulty.
     */
    private ObjectName getJMXName() throws MalformedObjectNameException
    {
        return new ObjectName("Zoom4J: name="+getName());
    }
    
    /**
     *  Empties the queue and calculates the results.
     */
    private void doLog(long lastRun)
    {
        if( m_log == null || !m_log.isInfoEnabled() ) return;
        
        StopWatch sw;
        
        m_statistics = new HashMap<String,CollectedStatistics>();
        
        while( null != (sw = m_queue.poll()) )
        {
            CollectedStatistics cs = m_statistics.get(sw.getTag());
            
            if( cs == null ) 
            {
                cs = new CollectedStatistics();
                m_statistics.put( sw.getTag(), cs );
            }
            
            cs.add( sw );
        }
        
        printf("Statistics from %tc to %tc", new Date(lastRun), new Date());
        
        printf("Tag                                       Avg(ms)      Min      Max  Std Dev   Count");
        
        for( Map.Entry<String,CollectedStatistics> e : m_statistics.entrySet() )
        {
            CollectedStatistics cs = e.getValue();
            printf("%-40s %8.2f %8.2f %8.2f %8.2f %7d", e.getKey(),cs.getAverageMS(), cs.getMin(), cs.getMax(), cs.getStdDev(), cs.getInvocations());
        }
        
        printf("");
    }
    
    /**
     *  Writes to the internal logger, just like ye goode olde C printf().
     *  
     *  @param pattern Pattern to write to (see {@link Formatter#format(String, Object...)}
     *  @param args Arguments for the pattern.
     */
    private void printf( String pattern, Object... args )
    {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        formatter.format(pattern, args);
        
        m_log.info(sb.toString());
    }
    
    /**
     *  An internal Thread which wakes up periodically and checks whether
     *  the data should be collected and dumped.
     */
    private class CollectorThread extends Thread
    {
        @Override
        public void run()
        {
            long lastRun = System.currentTimeMillis();
            
            // Round to the nearest periodSeconds
            lastRun = (lastRun / (1000*m_periodSeconds)) * (1000*m_periodSeconds);
            
            while(m_running)
            {
                try
                {
                    Thread.sleep(1000L);
                }
                catch(Throwable t)
                {
                    // Ignore all nasties, keep this thing running until requested.
                }                
                
                long now = System.currentTimeMillis();
                
                if( (now - lastRun)/1000 >= m_periodSeconds )
                {
                    doLog(lastRun);
                    lastRun = now;
                }
            }
            
            //
            // Do final log
            //
            doLog(lastRun);
        }
        
    }

    /**
     *  Set the logging period in seconds.  For example, a value of 5
     *  would log every 5 seconds, at 0,5,10,15,20,25,30,35,40,45,50, and 55 seconds
     *  after the full minute.
     *  
     *  @param string
     */
    public void setPeriod(String string)
    {
        m_periodSeconds = Integer.parseInt(string);
    }

    //
    //  START MBEAN STUFF HERE
    //
    
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        HashMap<String, CollectedStatistics> stats = m_statistics;
    
        if( stats != null )
        {
            String key     = attribute.substring(0,attribute.indexOf('/'));
            String postfix = attribute.substring(attribute.indexOf('/'));
            
            System.out.println("Key="+key+" postfix="+postfix);
            
            CollectedStatistics cs = m_statistics.get(key);
            
            if( postfix.equals(ATTR_POSTFIX_AVG))             
                return cs.getAverageMS();
            if( postfix.equals(ATTR_POSTFIX_MAX))
                return cs.getMax();
            if( postfix.equals(ATTR_POSTFIX_MIN))
                return cs.getMin();
            if( postfix.equals(ATTR_POSTFIX_STDDEV) )
                return cs.getStdDev();
            if( postfix.equals(ATTR_POSTFIX_COUNT) )
                return cs.getInvocations();
            
            throw new AttributeNotFoundException(attribute);
        }
        
        return null;
    }

    public AttributeList getAttributes(String[] attributes)
    {
        AttributeList ls = new AttributeList();
        
        for( String s : attributes )
        {
            try
            {
                ls.add( new Attribute(s,getAttribute(s)) );
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return ls;
    }

    public MBeanInfo getMBeanInfo()
    {
        return m_beanInfo;
    }

    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException
    {
        // This is a no-op, we don't set allow any ops
        return null;
    }

    public void setAttribute(Attribute attribute)
                                                 throws AttributeNotFoundException,
                                                     InvalidAttributeValueException,
                                                     MBeanException,
                                                     ReflectionException
    {
        // This is a no-op
    }

    public AttributeList setAttributes(AttributeList attributes)
    {
        // this is a no-op
        return null;
    }
    
    /**
     *  Builds the MBeanInfo for all the exposed attributes.
     *  
     *  @throws IntrospectionException
     */
    private void buildMBeanInfo() throws IntrospectionException
    {
        MBeanAttributeInfo[] attributes = null;

        if( m_jmxAttributes != null )
        {
            attributes = new MBeanAttributeInfo[m_jmxAttributes.length*5];

            for( int i = 0; i < m_jmxAttributes.length; i++ )
            {
                String name = m_jmxAttributes[i].trim();

                attributes[5*i]   = new MBeanAttributeInfo( name+ATTR_POSTFIX_AVG,    "double", "Average value (in milliseconds)", true, false, false );
                attributes[5*i+1] = new MBeanAttributeInfo( name+ATTR_POSTFIX_STDDEV, "double", "Standard Deviation", true, false, false );
                attributes[5*i+2] = new MBeanAttributeInfo( name+ATTR_POSTFIX_MIN,    "double", "Minimum value", true, false, false );
                attributes[5*i+3] = new MBeanAttributeInfo( name+ATTR_POSTFIX_MAX,    "double", "Maximum value", true, false, false );
                attributes[5*i+4] = new MBeanAttributeInfo( name+ATTR_POSTFIX_COUNT,  "int",    "Number of invocations", true, false, false );
            }
        }
        //
        //  Create the actual BeanInfo instance.
        //
        MBeanOperationInfo[] operations = null;
        MBeanConstructorInfo[] constructors = null;
        MBeanNotificationInfo[] notifications = null;

        m_beanInfo = new MBeanInfo( getClass().getName(),
                                    "PeriodicalLog for logger "+getName(),
                                    attributes,
                                    constructors,
                                    operations,
                                    notifications );
    }

}
