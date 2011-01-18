package com.ecyrd.zoom4j;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

public class PeriodicalLog extends Log
{
    private Queue<StopWatch> m_queue = new ConcurrentLinkedQueue<StopWatch>();
    private Logger m_systemLog;
    private Thread m_collectorThread;
    private boolean m_running = true;
    private int m_periodSeconds = 30;
    
    public PeriodicalLog(Logger systemLog)
    {
        m_systemLog = systemLog;
        
        m_collectorThread = new CollectorThread();
        
        m_collectorThread.start();
        
        m_systemLog.info("Logging!");
        
        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run()
            {
                shutdown();
            }
        });
    }
    
    public void log(StopWatch sw)
    {
        m_queue.add( sw.freeze() );
    }

    @Override
    public void shutdown()
    {
        m_running = false;
        m_collectorThread.interrupt();
    }
    
    /**
     *  Empties the queue and calculates the results.
     */
    private void doLog(long lastRun)
    {
        if( !m_systemLog.isInfoEnabled() ) return;
        
        StopWatch sw;
        
        HashMap<String,CollectedStatistics> coll = new HashMap<String,CollectedStatistics>();
        
        while( null != (sw = m_queue.poll()) )
        {
            CollectedStatistics cs = coll.get(sw.getTag());
            
            if( cs == null ) 
            {
                cs = new CollectedStatistics();
                coll.put( sw.getTag(), cs );
            }
            
            cs.add( sw );
        }
        
        printf("Statistics from %tc to %tc", new Date(lastRun), new Date());
        
        printf("Tag                                       Avg(ms)      Min      Max  Std Dev   Count");
        
        for( Map.Entry<String,CollectedStatistics> e : coll.entrySet() )
        {
            CollectedStatistics cs = e.getValue();
            printf("%-40s %8.2f %8.2f %8.2f %8.2f %7d", e.getKey(),cs.getAverageMS(), cs.getMin(), cs.getMax(), cs.getStdDev(), cs.getInvocations());
        }
        
        printf("");
    }
    
    private void printf( String pattern, Object... args )
    {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        formatter.format(pattern, args);
        
        m_systemLog.info(sb.toString());
    }
    
    private static final double NANOS_IN_MILLIS = 1e6;
    
    private static class CollectedStatistics
    {
        private List<Double> m_times = new ArrayList<Double>();
        private double m_min = Double.MAX_VALUE;
        private double m_max = 0.0;
        
        public void add(StopWatch sw)
        {
            double timeInMs = sw.getTimeNanos() / NANOS_IN_MILLIS;
            m_times.add(timeInMs);
            
            if( timeInMs < m_min ) m_min = timeInMs;
            if( timeInMs > m_max ) m_max = timeInMs;
        }
        
        public int getInvocations()
        {
            return m_times.size();
        }
        
        public double getMin()
        {
            return m_min;
        }
        
        public double getMax()
        {
            return m_max;
        }
        
        public double getAverageMS()
        {
            double result = 0.0;
        
            for( Double d : m_times )
            {
                result += d;
            }
            
            return result / m_times.size();
        }
        
        public double getStdDev()
        {
            return Math.sqrt(variance());
        }
        
        public double variance() 
        {
            long n = 0;
            double mean = 0;
            double s = 0.0;

            for (double x : m_times) 
            {
                n++;
                double delta = x - mean;
                mean += delta / n;
                s += delta * (x - mean);
            }

            return (s / n);
        }

    }
    
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
}
