package com.ecyrd.zoom4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopWatchFactory
{
    private static final Logger log = LoggerFactory.getLogger(StopWatchFactory.class);
    private static final String PROPERTY_PREFIX = "zoom4j";
    private static Properties c_config = new Properties();
    private static final String PROPERTYFILENAME = "zoom4j.properties";

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
            in = StopWatchFactory.class.getResourceAsStream("/com/ecyrd/zoom4j/default_zoom4j.properties");

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
            Class<Log> swfClass = (Class<Log>) Class.forName( className );

            Log log = swfClass.newInstance();
            
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
    
    public static class ConfigurationException extends RuntimeException
    {
        public ConfigurationException(Throwable rootCause)
        {
            super(rootCause);
        }
    }
    
    public StopWatch getStopWatch()
    {
        return getStopWatch(null,null);
    }
    
    public StopWatch getStopWatch( String tag )
    {
        return getStopWatch( tag,null );
    }
    
    public StopWatch getStopWatch( String tag, String message )
    {
        return new LoggingStopWatch( m_log, tag, message );
    }
    
    private void internalShutdown()
    {
        m_log.shutdown();
    }
    
    public static StopWatchFactory getDefault()
    {
        return new StopWatchFactory(null);
    }
    
    public static void shutdown()
    {
        for( StopWatchFactory swf : c_factories.values() )
        {
            swf.internalShutdown();
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
