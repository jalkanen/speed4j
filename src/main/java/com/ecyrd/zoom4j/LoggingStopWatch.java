package com.ecyrd.zoom4j;

import com.ecyrd.zoom4j.log.Log;

public class LoggingStopWatch extends StopWatch
{
    private static final long serialVersionUID = 1L;

    private Log m_log;
    
    protected LoggingStopWatch(Log log, String tag, String message)
    {
        super(tag, message);
        m_log = log;
    }

    protected void internalStop()
    {
        super.internalStop();
        
        // Do the logging here
        
        if( m_log != null && m_log.isEnabled() )
            m_log.log(this);
    }

    
}
