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
