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

import java.util.ArrayList;
import java.util.List;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.util.Percentile;

/**
 *  Represents a statistics item which contains data which has been collected
 *  over time.  It keeps track of maximum and minimum times, as well as a count,
 *  average and standard deviation.
 *  <p>
 *  Internally, it keeps the nanosecond accuracy, though most JVMs and operating
 *  systems can usually go down to microseconds only.  However, all API calls
 *  return milliseconds (as doubles), as it's the most useful figure.
 */
public class CollectedStatistics
{
    private List<Double> m_times = new ArrayList<Double>();
    private double m_min = Double.MAX_VALUE;
    private double m_max = 0.0;
    
    private double NANOS_IN_MILLIS = 1e6;
    
    /**
     *  Add a StopWatch to the statistics.
     *  
     *  @param sw StopWatch to add.
     */
    public void add(StopWatch sw)
    {
        double timeInMs = sw.getTimeNanos() / NANOS_IN_MILLIS;
        m_times.add(timeInMs);
        
        if( timeInMs < m_min ) m_min = timeInMs;
        if( timeInMs > m_max ) m_max = timeInMs;
    }
    
    /**
     *  Returns the number of stopwatches.  This is a fast operation.
     *  
     *  @return Number of StopWatches.
     */
    public int getInvocations()
    {
        return m_times.size();
    }
    
    /**
     *  Returns the fastest StopWatch time recorded.  This is a fast
     *  operation.
     *  
     *  @return Fastest in milliseconds.
     */
    public double getMin()
    {
        return m_min;
    }
    
    /**
     *  Returns the slowest StopWatch time recorded.  This is a fast
     *  operation.
     *  
     *  @return Slowest in milliseconds.
     */
    public double getMax()
    {
        return m_max;
    }
    
    /**
     *  Returns the average of the StopWatches recorded.  NB: This call
     *  causes all of the StopWatches to be traversed, which makes it fairly slow.
     *  
     *  @return The average in milliseconds.
     */
    public double getAverageMS()
    {
        double result = 0.0;
    
        for( Double d : m_times )
        {
            result += d;
        }
        
        return result / m_times.size();
    }
    
    /**
     *  Returns the standard deviation of all StopWatches recorded.  NB: This
     *  call causes all of the StopWatches to be traversed, which makes it fairly slow.
     *  
     *  @return The standard deviation.
     */
    public double getStdDev()
    {
        return Math.sqrt(variance());
    }
    
    /**
     *  Returns the variance of all StopWatches recorded. NB: This
     *  call causes all of the StopWatches to be traversed, which makes it fairly slow.
     *  
     *  @return The variance.
     */
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

    /**
     *  Get the nth percentile.  NB: This is also a fairly slow operation.
     *  
     *  @param percentile Percentile to get.
     *  @return
     */
    public double getPercentile(int percentile)
    {
        Percentile p = new Percentile();
        
        return p.evaluate( m_times, percentile );
    }
}