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
package com.ecyrd.speed4j;

import org.junit.Test;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.StopWatchFactory;
import com.ecyrd.speed4j.log.PeriodicalLog;
import com.ecyrd.speed4j.log.Slf4jLog;


public class StopWatchFactoryTest
{
    
    /* This succeeds if it passes quietly. */
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
    public void testSlf4jLog() throws InterruptedException
    {
        Slf4jLog log = new Slf4jLog();
        log.setSlf4jLogname("foo");
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
        log.setSlf4jLogname("foo");
        log.setPeriod(5);
        log.setName("testLog");
        log.setJmx("iteration:1,iteration:2,iteration:3,iteration:4");
        
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
