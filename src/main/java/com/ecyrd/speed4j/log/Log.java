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

/**
 *  Base class for the Logs.  A Log is an abstract entity that encapsulates
 *  the knowledge how to manage the information contained by StopWatches upon 
 *  their stop() method.
 */
public abstract class Log
{
    private static final String DEFAULT_NAME = "UndefinedLog";
    private boolean m_enable = true;
    private String  m_name   = DEFAULT_NAME;
    
    /**
     *  Enable or disable this Log.  If the log is disabled, no logging
     *  is done until it is enabled again.  By default, the Log is enabled
     *  when it's created (though obviously, some subclasses might make
     *  start disabled).
     *  
     *  @param value True, if enabled.  False otherwise.
     */
    public void setEnable(boolean value)
    {
        m_enable = value;
    }
    
    /**
     *  Returns true, if this Log is enabled.
     *  
     *  @return True or false.
     */
    public boolean isEnabled()
    {
        return m_enable;
    }
                   
    /**
     *  Sets the name of the Log.  If not set, uses {@value DEFAULT_NAME}.
     *  
     *  @param name Name of the Log.
     */
    public void setName(String name)
    {
        m_name = name;
    }
    
    /**
     *  Returns the name of the Log.
     *  
     *  @return The name of the Log.
     */
    public String getName()
    {
        return m_name;
    }
    
    /**
     *  Logs the given StopWatch.  Called when stop() is called.  In general,
     *  you will want to keep this method fairly speedy, since StopWatches
     *  are often used inside tight loops.
     *  
     *  @param sw The StopWatch to log.
     */
    public abstract void log(StopWatch sw);

    /**
     *  Shuts the Log down.  This can be used to free resources, etc.
     */
    public void shutdown()
    {
        // Default implementation does nothing
    }

}
