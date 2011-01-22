package com.ecyrd.zoom4j.log;

import com.ecyrd.zoom4j.StopWatch;

/**
 *  Base class for the Logs.
 *  
 */
public abstract class Log
{
    private boolean m_enable = true;
    private String  m_name   = "UndefinedLog";
    
    public void setEnable(String value)
    {
        if( value.equals("false") ) m_enable = false;
        else m_enable = true;
    }
    
    public boolean isEnabled()
    {
        return m_enable;
    }
                   
    public void setName(String name)
    {
        m_name = name;
    }
    
    public String getName()
    {
        return m_name;
    }
    
    public abstract void log(StopWatch sw);

    /**
     *  Shuts the Log down.  This can be used to free resources, etc.
     */
    public void shutdown()
    {
        // Default implementation does nothing
    }

}
