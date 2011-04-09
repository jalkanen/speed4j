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

import com.ecyrd.speed4j.log.Log;

/**
 *  A version of StopWatch which outputs the result upon stop() to the internal
 *  Log.  This is the version you most probably need.
 */
public class LoggingStopWatch extends StopWatch
{
    private static final long serialVersionUID = 1L;

    private Log m_log;
    
    /**
     *  Creates a LoggingStopWatch associated with the given Log.
     *  
     *  @param log The Log that is used by this StopWatch to output.
     *  @param tag The tag.
     *  @param message A free-form message.
     */
    protected LoggingStopWatch(Log log, String tag, String message)
    {
        super(tag, message);
        m_log = log;
    }

    /**
     *  If the log exists and is enabled, sends this StopWatch to the
     *  given Log to be logged.
     */
    protected void internalStop()
    {
        super.internalStop();
        
        // Do the logging here
        
        if( m_log != null && m_log.isEnabled() )
            m_log.log(this);
    }

    
}
