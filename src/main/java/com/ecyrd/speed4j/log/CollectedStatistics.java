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

class CollectedStatistics
{
    private List<Double> m_times = new ArrayList<Double>();
    private double m_min = Double.MAX_VALUE;
    private double m_max = 0.0;
    
    private double NANOS_IN_MILLIS = 1e6;
    
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