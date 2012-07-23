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
package com.ecyrd.speed4j.util;

import java.lang.reflect.Method;

/**
 *  Provides utility method to invoke a given setter on an object
 *  while smartly parsing the given argument.
 *  <p>
 *  Currently supports automatic detection of integers, booleans and strings. Is
 *  smart enough to recognize the unboxed versions as well (i.e. Integers are 
 *  treated equal to ints, Booleans like booleans, etc). 
 */
public final class SetterUtil
{
    private static Setter[] c_setters = 
    {
        new IntegerSetter(),
        new DoubleSetter(),
        new BooleanSetter(),
        new StringSetter()
    };
 
    /** Private constructor prevents accidental instantiation. */
    private SetterUtil() {}
    
    /**
     *  Sets the given parameter on the object while smartly parsing the value.
     *  For example, if the value can be interpreted as an integer, will invoke
     *  the setFoo(int) or setFoo(Integer) methods of the given object as opposed
     *  to just dummily invoking the setFoo(String) method.
     *  <p>
     *  The setFoo(String) method is always tried as the last resort.
     *  
     *  @param obj Object to invoke the setter on
     *  @param method The setter (e.g. "setFoo").
     *  @param value Value to set.
     *  @throws NoSuchMethodException If there is no applicable method to set this value.
     */
    public static void set( Object obj, String method, String value ) throws NoSuchMethodException
    {
        for( Setter s : c_setters )
        {
            try
            {
                s.set( obj, method, value );
                return;
            }
            catch(Exception e) {}
        }
        
        throw new NoSuchMethodException("No valid setter found for "+method+"(\""+value+"\")");
    }

    /**
     *  Defines the different Setters.
     */
    private interface Setter
    {
        /**
         *  Sets a value.  If it cannot set a value, it may throw any exception
         *  it wants.
         *  
         *  @param obj
         *  @param method
         *  @param value
         *  @throws Exception
         */
        public void set( Object obj, String method, String value ) throws Exception;
    }
    
    private static class IntegerSetter implements Setter
    {
        public void set( Object obj, String method, String value ) throws Exception
        {
            Class<? extends Object> c = obj.getClass();
            Object val;
            Method m;
            
            val = Integer.parseInt( value );
            try
            {
                m = c.getMethod( method, Integer.class );
            }
            catch( NoSuchMethodException e )
            {
                m = c.getMethod( method, int.class );
            }
                
            m.invoke( obj, val );
        }
    }

    private static class DoubleSetter implements Setter
    {
        public void set( Object obj, String method, String value ) throws Exception
        {
            Class<? extends Object> c = obj.getClass();
            Object val;
            Method m;

            val = Double.parseDouble( value );
            try
            {
                m = c.getMethod( method, Double.class );
            }
            catch( NoSuchMethodException e )
            {
                m = c.getMethod( method, double.class );
            }

            m.invoke( obj, val );
        }
    }
    
    private static class BooleanSetter implements Setter
    {

        public void set( Object obj, String method, String value ) throws Exception
        {
            Boolean b;
            Class<? extends Object> c = obj.getClass();
            b = value.equalsIgnoreCase( "true" ) ? true : (value.equalsIgnoreCase( "false" ) ? false : null);

            if( b == null ) throw new IllegalArgumentException();
            
            Method m;
            
            try
            {
                m = c.getMethod( method, Boolean.class );
            }
            catch( NoSuchMethodException e )
            {
                m = c.getMethod( method, boolean.class );
            }
            
            m.invoke( obj, b );
        }
        
    }


    private static class StringSetter implements Setter
    {
        public void set( Object obj, String method, String value ) throws Exception
        {
            Method m = obj.getClass().getMethod( method, String.class );
            m.invoke( obj, value );
        }
        
    }
}
