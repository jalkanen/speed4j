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
    private DoubleList m_times = new DoubleList();
    private double m_min = Double.MAX_VALUE;
    private double m_max = 0.0;

    private double MICROS_IN_MILLIS = 1e3;

    /**
     *  Add a StopWatch to the statistics.
     *
     *  @param sw StopWatch to add.
     */
    public synchronized void add(StopWatch sw)
    {
        double timeInMs = sw.getTimeMicros() / MICROS_IN_MILLIS;
        m_times.add(timeInMs);

        if( timeInMs < m_min ) m_min = timeInMs;
        if( timeInMs > m_max ) m_max = timeInMs;
    }

    /**
     *  Returns the number of stopwatches.  This is a fast operation.
     *
     *  @return Number of StopWatches.
     */
    public synchronized int getInvocations()
    {
        return m_times.size();
    }

    /**
     *  Returns the fastest StopWatch time recorded.  This is a fast
     *  operation.
     *
     *  @return Fastest in milliseconds.
     */
    public synchronized double getMin()
    {
        return m_min;
    }

    /**
     *  Returns the slowest StopWatch time recorded.  This is a fast
     *  operation.
     *
     *  @return Slowest in milliseconds.
     */
    public synchronized double getMax()
    {
        return m_max;
    }

    /**
     *  Returns the average of the StopWatches recorded.  NB: This call
     *  causes all of the StopWatches to be traversed, which makes it fairly slow.
     *
     *  @return The average in milliseconds.
     */
    public synchronized double getAverageMS()
    {
        double result = 0.0;

        for( Double d : m_times.m_values )
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
    public synchronized double getStdDev()
    {
        return Math.sqrt(variance());
    }

    /**
     *  Returns the variance of all StopWatches recorded. NB: This
     *  call causes all of the StopWatches to be traversed, which makes it fairly slow.
     *
     *  @return The variance.
     */
    public synchronized double variance()
    {
        long n = 0;
        double mean = 0;
        double s = 0.0;

        for (double x : m_times.m_values)
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
    public synchronized double getPercentile(int percentile)
    {
        return getPercentile( (double)percentile );
    }

    /**
     *  Get the nth percentile.  NB: This is also a fairly slow operation.
     *
     *  @param percentile Percentile to get.
     *  @return
     */
    public double getPercentile( double percentile )
    {
        Percentile p = new Percentile();

        return p.evaluate( m_times.m_values, 0, m_times.size(), percentile );
    }

    /**
     *  A very simple class to hold a number of double values in memory fairly
     *  optimally (this is better than using a Double array, since Doubles take
     *  an extra 8 bytes of overhead per instance compared to regular double).
     */
    private static final class DoubleList
    {
        public double[] m_values = new double[256];
        public int      m_size;

        public void add(double d)
        {
            ensureCapacity(m_size+1);
            m_values[m_size++] = d;
        }

        public int size()
        {
            return m_size;
        }

        private void ensureCapacity(int capacity)
        {
            if( capacity > m_values.length )
            {
              int newsize = ((m_values.length * 3) / 2) + 1;
              double[] olddata = m_values;
              m_values = new double[newsize < capacity ? capacity : newsize];
              System.arraycopy(olddata, 0, m_values, 0, m_size);
            }
        }
    }
}