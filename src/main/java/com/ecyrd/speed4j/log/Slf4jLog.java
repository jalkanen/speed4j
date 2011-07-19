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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;

/**
 *  A simple logger which does nothing except write the stopwatch data to
 *  the given SLF4J logger using {@link Logger#info(String)}.
 */
public class Slf4jLog extends Log
{
    /**
     *  Stores the SLF4J logger instance.
     */
    protected Logger m_log;
        
    /**
     *  Set the name of the logger that this logger should log to.
     *  If you set it to an empty string, will shut up completely.
     */
    public void setSlf4jLogname(String logName)
    {
        if( logName.isEmpty() ) 
        {
            m_log = null;
        }
        else
        {
            m_log = LoggerFactory.getLogger(logName);
        }
    }
        
    /**
     *  Logs using the INFO priority.
     */
    @Override
    public void log(StopWatch sw)
    {
        //
        //  This avoids calling the possibly expensive sw.toString() method if logging is disabled.
        //
        if( m_log != null && m_log.isInfoEnabled() )
        {
            m_log.info(sw.toString());
        }
    }

}
