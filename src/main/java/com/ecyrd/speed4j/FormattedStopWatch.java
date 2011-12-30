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

/**
 *  Provides a version of StopWatch which outputs the performance message
 *  using a very specific format which can then be easily interpreted
 *  with scripts.
 */
public class FormattedStopWatch extends StopWatch
{
    /**
     *  Returns the tag & message in a fixed format.  The format
     *  is simply
     *  <pre>
     *    time[TIME] tag[TAG] message[MESSAGE]
     *  </pre>
     *
     *  where TIME is the elapsed time in microseconds, the TAG is the tag
     *  for the StopWatch and MESSAGE is the message. If the message is empty,
     *  an empty "message[]" will be emitted.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "time[" ).append(getTimeMicros()).append("] tag[")
          .append( getTag() ).append( "] message[" ).append( getMessage() ).append(']');

        return sb.toString();
    }
}
