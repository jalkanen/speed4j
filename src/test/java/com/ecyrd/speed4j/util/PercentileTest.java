package com.ecyrd.speed4j.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

public class PercentileTest
{
    @Test
    public void testPercentile()
    {
        ArrayList<Double> t = new ArrayList<Double>();
        
        for( int i = 0; i < 100; i++ ) t.add( (double) i );
        
        Percentile p = new Percentile();
        
        //
        //  Since these are only estimates for p, we have to 
        //  be a bit rough about them.
        //
        assertEquals( 95.0, p.evaluate( t, 95 ), 0.5 );
        assertEquals( 50.0, p.evaluate( t, 50 ), 0.5 );
    }
}
