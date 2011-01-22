package com.ecyrd.zoom4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A simple logger which does nothing except write the stopwatch data to
 *  the given SLF4J logger using {@link Logger#info(String)}
 *  
 *  @author jalkanen
 *
 */
public class Slf4jLog extends Log
{
    protected Logger m_log;
        
    /**
     *  Set the name of the logger that this logger should log to.
     */
    public void setSlf4Logname(String logName)
    {
        m_log = LoggerFactory.getLogger(logName);
    }
        
    @Override
    public void log(StopWatch sw)
    {
        //
        //  This avoids calling the possibly expensive sw.toString() method if logging is disabled.
        //
        if( m_log.isInfoEnabled() )
        {
            m_log.info(sw.toString());
        }
    }

}
