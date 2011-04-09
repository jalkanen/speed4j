package com.ecyrd.speed4j.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SetterUtilTest
{
    @Test
    public void testStringSetter() throws NoSuchMethodException
    {
        TestCallable tc = new TestCallable();
        
        SetterUtil.set( tc, "setString", "test" );
        
        assertEquals( "test", tc.stringField );
    }

    @Test
    public void testBoolSetter() throws NoSuchMethodException
    {
        TestCallable tc = new TestCallable();
        
        SetterUtil.set( tc, "setBoolean", "true" );
        
        assertEquals( true, tc.booleanField );

        SetterUtil.set( tc, "setBool", "false" );

        assertEquals( false, tc.booleanField );
    }

    @Test
    public void testIntSetter() throws NoSuchMethodException
    {
        TestCallable tc = new TestCallable();
        
        SetterUtil.set( tc, "setInt", "50" );
        
        assertEquals( (Integer)50, tc.integerField );

        SetterUtil.set( tc, "setInteger", "100" );

        assertEquals( (Integer)100, tc.integerField );
    }

    public static class TestCallable
    {
        public String stringField;
        public Integer integerField;
        public Boolean booleanField;
        
        public void setString(String s)
        {
            stringField = s;
        }
        
        public void setInteger(Integer i)
        {
            integerField = i;
        }
        
        public void setInt(int i)
        {
            integerField = i;
        }
        
        public void setBoolean( Boolean b )
        {
            booleanField = b;
        }
        
        public void setBool(boolean b)
        {
            booleanField = b;
        }
    }
}
