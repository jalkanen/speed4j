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
package com.ecyrd.speed4j.log;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.ConfigurationException;
import com.ecyrd.speed4j.StopWatch;

/**
 *  A Periodical log which can also expose its attributes via JMX.
 *  <p>
 *  The JMX name is based on the name of the Log.  So if you don't set
 *  it via {@link #setName(String)}, you'll end up something that Speed4J
 *  picks up on its own.  Normally, if you use the property file to
 *  configure Speed4J, this gets automatically assigned for you.
 *  <p>
 *  The PeriodicalLog starts a new Thread to collect the statistics. Don't forget to shut it down with a call to {@link #shutdown()},
 *  or else you might risk a memory leak.  This is a common problem with e.g. web applications, where redeployment
 *  regularly causes these.
 *  <p>
 *  In a web app, you could set up your own ServletContextListener to ensure the proper shutdown:
 *  <pre>
 *  class MyListener implements ServletContextListener {
 *     public void contextInitialized(ServletContextEvent sce) {}
 *
 *     public void contextDestroyed(ServletContextEvent sce) {
 *         StopWatchFactory.getInstance("myLoggerName").shutdown();
 *     }
 *  }
 *  </pre>
 *  PeriodicalLog adds its own JVM-wide shutdown hook, so you don't need to prepare for that.
 *  <p>
 *  The size of the log is limited to make sure that it does not grow out of bounds.  You
 *  may set the value using {@link #setMaxQueueSize(int)}.  The default value is {@value #DEFAULT_MAX_QUEUE_SIZE}.
 */
public class PeriodicalLog extends Slf4jLog implements DynamicMBean
{
    private static final String JMX_QUEUE_LENGTH = "StopWatchQueueLength";
    private static final String JMX_DROPPED_STOPWATCHES = "DroppedStopWatches";
    private static final String JMX_PERIOD_SECONDS = "LoggingPeriod";
    
    private static final int    CONSTANT_ATTRS_PER_ITEM = 5;
    private static final String ATTR_POSTFIX_MAX = "/max";
    private static final String ATTR_POSTFIX_MIN = "/min";
    private static final String ATTR_POSTFIX_STDDEV = "/stddev";
    private static final String ATTR_POSTFIX_AVG = "/avg";
    private static final String ATTR_POSTFIX_COUNT = "/count";

    private static final int DEFAULT_MAX_QUEUE_SIZE = 300000;

    private LinkedBlockingDeque<StopWatch> m_queue = new LinkedBlockingDeque<StopWatch>(DEFAULT_MAX_QUEUE_SIZE);
    private CollectorThread  m_collectorThread;
    private int              m_periodSeconds      = 30;
    private boolean          m_stopCollector      = false;
    private MBeanServer      m_mbeanServer        = null;
    private String[]         m_jmxAttributes      = null;
    private MBeanInfo        m_beanInfo;
    private AtomicLong       m_rejectedStopWatches = new AtomicLong();
    private ObjectName       m_jmxName;
    private Map<String,JmxStatistics>       m_jmxStatistics;
    private Map<String,CollectedStatistics> m_stats = new HashMap<String,CollectedStatistics>();
    private double[]         m_percentiles = { 95 };
    
    private static Logger    log = LoggerFactory.getLogger( PeriodicalLog.class );

    private Mode             m_mode = Mode.ALL;

    /**
     *  Creates an instance of PeriodicalLog.
     */
    public PeriodicalLog()
    {
        m_collectorThread = new CollectorThread();
        m_collectorThread.setName( "Speed4J PeriodicalLog Collector Thread" );
        m_collectorThread.setDaemon( true );
        m_collectorThread.start();

        rebuildJmx();

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
        if( !m_queue.offer( sw.freeze() ) ) m_rejectedStopWatches.getAndIncrement();
    }

    /**
     *  Set the tags which are shown via JMX.  As a side-effect, will unregister
     *  and reregister the JMX Management Bean, so you can set these at runtime.
     *
     *  @param value A comma-separated list of tags which are shown via JMX.
     */
    public void setJmx( String value )
    {
        m_jmxAttributes = value.split(",");

        rebuildJmx();
    }

    /**
     *  Sets the percentiles that should be measured.  This is a
     *  comma-separated list of percentiles, e.g. "95, 99, 99.9".
     *  
     *  @param percentiles Comma-separated list of percentiles.
     */
    public void setPercentiles( String percentiles )
    {
        if( percentiles == null ) percentiles = "";
        
        String[] percList = percentiles.split(",");
        double[] percs = new double[percList.length];
        
        for( int i = 0; i < percList.length; i++ )
            percs[i] = Double.parseDouble( percList[i] );
        
        m_percentiles = percs;
        
        rebuildJmx();
    }
    
    /**
     *  You may set the mode for the PeriodicalLog.  This can be
     *  <ul>
     *    <li>QUIET - when you don't want any logging</li>
     *    <li>JMX_ONLY - when you want JMX only to show up</li>
     *    <li>LOG_ONLY - when you want just log, not JMX</li>
     *    <li>ALL - When you want both JMX and log</li>
     *  </ul>
     *  <p>
     *  By default PeriodicalLog is in ALL mode.
     *
     *  @param mode One of the above strings.  If the string cannot
     *              be recognized, the value is ignored.
     */
    public void setMode( String mode )
    {
        Mode m = Mode.valueOf( mode );

        if( m != null )
            m_mode = m;
    }

    /**
     *  You can set the mode also directly, see {@link #setMode(String)}.
     *
     *  @param mode Mode to set.
     */
    public void setMode( Mode mode )
    {
        if( mode != null )
        {
            m_mode = mode;
            rebuildJmx();
        }
    }

    /**
     *  For limiting the queue size in case the calculation is simply too slow
     *  and events are gathering too fast.  Once the queue becomes too big,
     *  events are silently dropped, and the JMX attribute {@value #JMX_DROPPED_STOPWATCHES} will be
     *  incremented.
     *  <p>
     *  Note that setting this value will discard the existing events in the queue,
     *  so while you can set this while speed4j is running, you will lose some data.
     *  <p>
     *  The default size is {@value #DEFAULT_MAX_QUEUE_SIZE}.
     *
     *  @param size The new maximum size.
     */
    public void setMaxQueueSize(int size)
    {
        m_queue = new LinkedBlockingDeque<StopWatch>(size);
    }

    /**
     *  Rebuild the JMX bean.
     */
    private void rebuildJmx()
    {
        m_mbeanServer = ManagementFactory.getPlatformMBeanServer();

        try
        {
            buildMBeanInfo();

            //
            //  Remove and reinstall from the MBean registry if it already exists.  Also
            //  remove previous instances.
            //
            ObjectName newName = getJMXName();

            if( m_mbeanServer.isRegistered(newName) )
            {
                log.debug("Removing already registered bean {}", newName);
                m_mbeanServer.unregisterMBean(newName);
            }

            if( m_jmxName != null && !newName.equals(m_jmxName) && m_mbeanServer.isRegistered(m_jmxName) )
            {
                log.debug("Removing the old bean {}", m_jmxName);
                m_mbeanServer.unregisterMBean(m_jmxName);
            }

            m_jmxName = getJMXName();

            if( m_beanInfo != null )
            {
                m_mbeanServer.registerMBean( this, m_jmxName );

                log.debug("Registered new JMX bean '{}'", m_jmxName);
            }
        }
        catch (InstanceAlreadyExistsException e)
        {
            log.debug("JMX bean '{}' already registered, continuing...");
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    /**
     *  Shuts down the collector thread and removes the JMX bean
     *  if it is registered.  It is <i>very</i> important to call this
     *  or else you risk a memory leak.
     */
    @Override
    public void shutdown()
    {
        m_stopCollector = true;
        if( m_collectorThread != null ) m_collectorThread.interrupt();

        try
        {
            //
            //  Remove MBean
            //
            if( m_mbeanServer != null && m_mbeanServer.isRegistered(getJMXName()) )
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
        return new ObjectName("Speed4J: name="+getName());
    }

    /**
     *  Empties the StopWatch queue, and builds the statistics on the go.
     *  This is run in the Collector Thread context.  Leaves the queue
     *  when the items in it start later than what we need.
     *
     *  @return true, if the queue is not emptied and there are events
     *          in it where the finalMoment is achieved.
     */
    private boolean emptyQueue(long finalMoment)
    {
        StopWatch sw;

        try
        {
            while( null != (sw = m_queue.poll(100, TimeUnit.MILLISECONDS)) )
            {
                if( sw.getCreationTime() > finalMoment )
                {
                    // Return this one back in the queue; as it belongs to the
                    // next period already.
                    m_queue.addFirst(sw);
                    return true;
                }

                CollectedStatistics cs = m_stats.get(sw.getTag());

                if( cs == null )
                {
                    cs = new CollectedStatistics();
                    m_stats.put( sw.getTag(), cs );
                }

                cs.add( sw );
            }
        }
        catch( InterruptedException e ) {} // Just return immediately

        return false;
    }

    /**
     *  Empties the queue and calculates the results.
     *  Thread-safety is done as follows: In order to avoid concurrent modifications,
     *  we have a thread-safe Queue object.  We pull StopWatches from the head of
     *  the queue, at which point we become the sole owners of the object.
     *  <p>
     *  If the queue has objects which are newer than what we're supposed to handle,
     *  we leave them in the queue and stop processing at that time.
     *  <p>
     *  This should be the only method that changes the statistics object, so it does
     *  not require locking either.
     */
    // TODO: This method is too slow since it does recalculate things too often
    private void doLog(long lastRun, long finalMoment)
    {
        double[] percentilenames = m_percentiles;

        //
        //  Do logging, if requested.
        //
        if( m_mode == Mode.LOG_ONLY || m_mode == Mode.ALL )
        {
            if( m_log != null && m_log.isInfoEnabled() )
            {
                StringBuilder percString = new StringBuilder();
                for( int i = 0; i < percentilenames.length; i++ )
                {
                    percString.append( String.format( " %6sth", Double.toString(percentilenames[i]) ) );
                }
                
                printf("Statistics from %tc to %tc", new Date(lastRun), new Date(finalMoment));

                printf("Tag                                                           Avg(ms)      Min      Max  Std Dev"+percString+"  Count");

                for( Map.Entry<String,CollectedStatistics> e : m_stats.entrySet() )
                {
                    CollectedStatistics cs = e.getValue();
                    StringBuilder sb = new StringBuilder();
                    
                    sb.append(String.format("%-60s %8.2f %8.2f %8.2f %8.2f", e.getKey(),cs.getAverageMS(), cs.getMin(), cs.getMax(), cs.getStdDev()));
                    for( int i = 0; i < percentilenames.length; i++ )
                        sb.append( String.format(" %8.2f", cs.getPercentile( percentilenames[i] )) );
                    
                    sb.append( String.format("%7d", cs.getInvocations()) );
                    m_log.info( sb.toString() );
                }
                
                m_log.info( "" );
            }
        }

        //
        //  Store these to the JMX attribute list
        //

        if( m_mode == Mode.JMX_ONLY || m_mode == Mode.ALL )
        {
            if( m_jmxAttributes != null )
            {
                m_jmxStatistics = new ConcurrentHashMap<String, PeriodicalLog.JmxStatistics>();

                for( String name : m_jmxAttributes )
                {
                    String n = name.trim();
                    CollectedStatistics cs = m_stats.get(n);

                    if ( cs == null )
                        continue;

                    JmxStatistics js = new JmxStatistics();
                    js.count = cs.getInvocations();
                    js.max = cs.getMax();
                    js.min = cs.getMin();
                    js.mean = cs.getAverageMS();
                    js.percentiles = new double[percentilenames.length];
                    
                    for( int i = 0; i < percentilenames.length; i++ )
                        js.percentiles[i] = cs.getPercentile( percentilenames[i] );
                    
                    js.stddev = cs.getStdDev();
                    m_jmxStatistics.put(n, js);
                }
            }
        }

        resetForNextPeriod();
    }

    private void resetForNextPeriod()
    {
        m_stats.clear();
    }

    /**
     *  Writes to the internal logger, just like ye goode olde C printf().
     *
     *  @param pattern Pattern to write to (see {@link Formatter#format(String, Object...)}
     *  @param args Arguments for the pattern.
     */
    private void printf( String pattern, Object... args )
    {
        if( m_log != null )
        {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);

            try
            {
                formatter.format(pattern, args);

                m_log.info(sb.toString());
            }
            finally
            {
                formatter.close();
            }
        }
    }

    /**
     *  An internal Thread which wakes up periodically and checks whether
     *  the data should be collected and dumped.
     */
    private class CollectorThread extends Thread
    {
        long m_lastRun = System.currentTimeMillis();
        long m_nextWakeup;

        void nextPeriod()
        {
            long now = System.currentTimeMillis();

            long periodMillis = m_periodSeconds * 1000L;
            m_nextWakeup = (now / periodMillis) * periodMillis + periodMillis;
        }

        @Override
        public void run()
        {
            nextPeriod();

            while(!m_stopCollector)
            {
                try
                {
                    if( emptyQueue(m_nextWakeup) )
                    {
                        doLog(m_lastRun, m_nextWakeup);
                        m_lastRun = m_nextWakeup;
                        nextPeriod();
                    }
                }
                catch( Throwable t )
                {
                    // Make sure that we keep running no matter what.
                    // TODO: Log this?
                }
            }

            long now = System.currentTimeMillis();
            emptyQueue( now );
            doLog( m_lastRun, now );
        }

    }

    /**
     *  Set the logging period in seconds.  For example, a value of 5
     *  would log every 5 seconds, at 0,5,10,15,20,25,30,35,40,45,50, and 55 seconds
     *  after the full minute.
     *
     *  @param periodSeconds The period in seconds.
     */
    public void setPeriod(int periodSeconds)
    {
        m_periodSeconds = periodSeconds;
        m_collectorThread.nextPeriod();
    }


    @Override
    public void setName(String name)
    {
        super.setName(name);

        rebuildJmx();
    }

    //
    //  START MBEAN STUFF HERE
    //

    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        if( attribute.equals( JMX_QUEUE_LENGTH ) )
            return m_queue.size();
        else if( attribute.equals( JMX_DROPPED_STOPWATCHES) )
            return m_rejectedStopWatches.longValue();
        else if( attribute.equals( JMX_PERIOD_SECONDS ) )
            return m_periodSeconds;
        
        Map<String, JmxStatistics> stats = m_jmxStatistics;

        if( stats != null )
        {
            String key     = attribute.substring(0,attribute.lastIndexOf('/'));
            String postfix = attribute.substring(attribute.lastIndexOf('/'));

            //System.out.println("Key="+key+" postfix="+postfix);

            JmxStatistics cs = stats.get(key);

            if( cs == null ) return null; // No value yet.

            if( postfix.equals(ATTR_POSTFIX_AVG))
                return cs.mean;
            if( postfix.equals(ATTR_POSTFIX_MAX))
                return cs.max;
            if( postfix.equals(ATTR_POSTFIX_MIN))
                return cs.min;
            if( postfix.equals(ATTR_POSTFIX_STDDEV) )
                return cs.stddev;
            if( postfix.equals(ATTR_POSTFIX_COUNT) )
                return cs.count;

            try
            {
                double n = Double.parseDouble( postfix.substring(1) );
                for( int i = 0; i < m_percentiles.length; i++ )
                {
                    if( Math.abs( m_percentiles[i] - n ) < 0.00001 )
                        return cs.percentiles[i];
                }
            }
            catch( NumberFormatException e )
            {
                System.out.println("Fail:");
                e.printStackTrace();
            }
            
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
        if( attribute.getName().equals(JMX_PERIOD_SECONDS) )
            m_periodSeconds = ((Number)attribute.getValue()).intValue();
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
        if( m_mode == Mode.JMX_ONLY || m_mode == Mode.ALL )
        {
            MBeanAttributeInfo[] attributes = null;

            int numAttrs = m_jmxAttributes != null ? m_jmxAttributes.length : 0;

            int numItems = CONSTANT_ATTRS_PER_ITEM + m_percentiles.length;

            attributes = new MBeanAttributeInfo[numAttrs*numItems+3];

            if( m_jmxAttributes != null )
            {
                for( int i = 0; i < m_jmxAttributes.length; i++ )
                {
                    String name = m_jmxAttributes[i].trim();

                    attributes[numItems*i]   = new MBeanAttributeInfo( name+ATTR_POSTFIX_AVG,    "double", "Average value (in milliseconds)", true, false, false );
                    attributes[numItems*i+1] = new MBeanAttributeInfo( name+ATTR_POSTFIX_STDDEV, "double", "Standard Deviation", true, false, false );
                    attributes[numItems*i+2] = new MBeanAttributeInfo( name+ATTR_POSTFIX_MIN,    "double", "Minimum value", true, false, false );
                    attributes[numItems*i+3] = new MBeanAttributeInfo( name+ATTR_POSTFIX_MAX,    "double", "Maximum value", true, false, false );
                    attributes[numItems*i+4] = new MBeanAttributeInfo( name+ATTR_POSTFIX_COUNT,  "int",    "Number of invocations", true, false, false );

                    //
                    //  Generate the percentile titles as /<perc>
                    //  Drops the fractions if they're zero.
                    //  TODO: There's probably a prettier way to do this.
                    //
                    for( int p = 0; p < m_percentiles.length; p++ )
                    {
                        String perTitle = Double.toString( m_percentiles[p] );
                        if( perTitle.endsWith(".0") ) perTitle = perTitle.substring( 0, perTitle.length()-2 );

                        attributes[numItems*i+5+p] = new MBeanAttributeInfo( name+"/"+perTitle,  "double", perTitle+"th percentile", true, false, false );
                    }
                }

            }

            //
            //  Add internal management attributes if there's a need for
            //  JMX
            //
            attributes[attributes.length-1] = new MBeanAttributeInfo( JMX_QUEUE_LENGTH, "int",
                                                                      "Current StopWatch processing queue length (i.e. how many StopWatches are currently unprocessed)",
                                                                      true, false, false );

            attributes[attributes.length-2] = new MBeanAttributeInfo( JMX_DROPPED_STOPWATCHES, "long",
                                                                      "How many StopWatches have been dropped due to processing restrictions",
                                                                      true, false, false );

            attributes[attributes.length-3] = new MBeanAttributeInfo( JMX_PERIOD_SECONDS, "int",
                                                                      "Current logging period (seconds)",
                                                                      true, true, false );

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
        else
        {
            m_beanInfo = null;
        }

    }

    private static class JmxStatistics
    {
        public double mean;
        public double stddev;
        public double min;
        public double max;
        public int    count;
        public double[] percentiles;
    }

    /**
     *  Describes the possible modes in which this system can be.
     */
    public static enum Mode
    {
        QUIET,
        JMX_ONLY,
        LOG_ONLY,
        ALL
    }
}
