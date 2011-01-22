package com.ecyrd.zoom4j;

/**
 *  Base class for the Logs.
 *  
 */
public abstract class Log
{
    public abstract void log(StopWatch sw);

    /**
     *  Shuts the Log down.  This can be used to free resources, etc.
     */
    public void shutdown()
    {
        // Default implementation does nothing
    }

}
