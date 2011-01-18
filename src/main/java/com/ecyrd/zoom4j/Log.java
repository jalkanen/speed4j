package com.ecyrd.zoom4j;

public abstract class Log
{
    public abstract void log(StopWatch sw);

    public void shutdown()
    {
        // Default implementation does nothing
    }
}
