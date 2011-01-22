package com.ecyrd.zoom4j.log;

import java.util.ArrayList;
import java.util.List;

import com.ecyrd.zoom4j.StopWatch;

class CollectedStatistics
{
    private List<Double> m_times = new ArrayList<Double>();
    private double m_min = Double.MAX_VALUE;
    private double m_max = 0.0;
    
    public void add(StopWatch sw)
    {
        double timeInMs = sw.getTimeNanos() / PeriodicalLog.NANOS_IN_MILLIS;
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