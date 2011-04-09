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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.log.Log;

public class StopWatchFactory
{
    private static final Logger log = LoggerFactory.getLogger(StopWatchFactory.class);
    private static final String PROPERTY_PREFIX = "speed4j";
    private static Properties c_config = new Properties();
    private static final String PROPERTYFILENAME = "speed4j.properties";

    private static Map<String,StopWatchFactory> c_factories = new HashMap<String,StopWatchFactory>();

    private Log m_log;
    
    /**
     *  When the class is instantiated, try to set up the configuration from the config
     *  file.
     */
    static
    {
        configure();
    }
    
    /**
     *  Load configuration file, try to parse it and do something useful.
     *  
     *  @throws ConfigurationException If configuration fails.
     */
    private static void configure() throws ConfigurationException
    {
        InputStream in = StopWatchFactory.class.getResourceAsStream(PROPERTYFILENAME);
        
        if( in == null )
            in = StopWatchFactory.class.getResourceAsStream("/"+PROPERTYFILENAME);
        
        if( in == null )
            in = StopWatchFactory.class.getResourceAsStream("/com/ecyrd/speed4j/default_speed4j.properties");

        try
        {
            c_config.load(in);
        }
        catch (IOException e2)
        {
            throw new ConfigurationException(e2);
        }

        for( Enumeration<String> e = (Enumeration<String>) c_config.propertyNames(); e.hasMoreElements(); )
        {
            String key = e.nextElement();

            String[] components = key.split("\\.");

            if( components.length < 2 || !components[0].equals(PROPERTY_PREFIX) ) continue;

            String logger = components[1];
            
            StopWatchFactory swf = c_factories.get(logger);
            
            if( swf == null )
            {
                swf = new StopWatchFactory( instantiateLog(logger) );
            }
            
            //
            //  Call the respective setXXX() methods of the logger
            //  based on the configuration.
            //
            //  TODO: Be smart and do e.g. String->boolean conversion, etc.
            //
            if( components.length > 2 )
            {
                String setting = components[2];
                
                String method = "set"+Character.toUpperCase(setting.charAt(0))+setting.substring(1);
                
                try
                {
                    Method m = swf.getLog().getClass().getMethod(method,String.class);
                    
                    m.invoke(swf.getLog(), c_config.get(key));
                }
                catch( NoSuchMethodException e1 )
                {
                    log.warn("An unknown setting {} for logger {}, wasn't able to find {}.{}.  Continuing nevertheless.", 
                             new Object[] {setting, logger, logger, method} );
                }
                catch (Exception e1)
                {
                    throw new ConfigurationException(e1);
                }
            }
            
            c_factories.put(logger, swf);
        }
    }

    /**
     *  Create a {@link StopWatchFactory} using the given Log.
     *  
     *  @param logger The log to use.
     */
    public StopWatchFactory( Log logger )
    {
        m_log = logger;
    }
    
    private Log getLog()
    {
        return m_log;
    }
    
    private static Log instantiateLog(String logger) throws ConfigurationException
    {
        String className = c_config.getProperty(PROPERTY_PREFIX+"."+logger);
        try
        {
            @SuppressWarnings("unchecked")
            Class<Log> swfClass = (Class<Log>) Class.forName( className );

            Log log = swfClass.newInstance();
            
            log.setName(logger);
            
            return log;
        }
        catch (ClassNotFoundException e1)
        {
            log.error("Configuration problem: I was unable to locate class {}, defined for logger {}", className, logger );
            throw new ConfigurationException(e1);
        }
        catch (InstantiationException e)
        {
            log.error("Configuration problem: I was unable to instantiate class {}, defined for logger {}", className, logger );
            throw new ConfigurationException(e);
        }
        catch (IllegalAccessException e)
        {
            log.error("Configuration problem: I am not allowed to access class {}, defined for logger {}", className, logger );
            throw new ConfigurationException(e);
        }
    }
    
    /**
     *  Return a StopWatch for a null tag and null message.
     *  
     *  @return A configured StopWatch.
     */
    public StopWatch getStopWatch()
    {
        return getStopWatch(null,null);
    }
    
    /**
     *  Returns a StopWatch for the given tag and null message.
     *  
     *  @param tag Tag which identifies this StopWatch.
     *  @return A new StopWatch instance.
     */
    public StopWatch getStopWatch( String tag )
    {
        return getStopWatch( tag,null );
    }
    
    /**
     *  Returns a StopWatch for the given tag and given message.
     *   
     *  @param tag Tag which identifies this StopWatch.
     *  @param message A free-form message.
     *  @return A new StopWatch.
     */
    public StopWatch getStopWatch( String tag, String message )
    {
        return new LoggingStopWatch( m_log, tag, message );
    }
    
    private void internalShutdown()
    {
        m_log.shutdown();
    }
    
    /**
     *  Returns the default StopWatchFactory, which contains no
     *  Log configuration.
     *  
     *  @return The default StopWatchFactory.
     */
    public static StopWatchFactory getDefault()
    {
        return new StopWatchFactory(null);
    }
    
    /**
     *  Shut down all StopWatchFactories.  This method is useful
     *  to call to clean up any resources which might be usable.
     */
    public static void shutdown()
    {
        for( Iterator<Entry<String, StopWatchFactory>> i = c_factories.entrySet().iterator(); i.hasNext() ; )
        {
            Map.Entry<String,StopWatchFactory> e = i.next();
            
            StopWatchFactory swf = e.getValue();
            swf.internalShutdown();
            
            i.remove();
        }
    }

    /**
     *  Return new StopWatchFactory which uses the specified Log.  You are fully
     *  responsible for this StopWatchFactory for now, i.e. shutdown() does not
     *  use this.
     *  
     *  @param logger
     *  @return
     */
    public static StopWatchFactory getInstance(Log logger)
    {
        return new StopWatchFactory(logger);
    }
    
    /**
     *  Returns a StopWatchFactory that has been configured previously. May return 
     *  null, if the factory has not been configured.
     *  
     *  @param loggerName name to search for.
     *  @return A factory, or null, if not found.
     */
    public static StopWatchFactory getInstance(String loggerName)
    {
        return c_factories.get(loggerName);
    }

}
