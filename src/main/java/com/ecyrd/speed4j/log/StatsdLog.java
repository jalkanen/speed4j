/*
   Copyright 2012 Patrick Ting (https://github.com/pcting)

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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.log.Slf4jLog;

public class StatsdLog extends Slf4jLog {

    private static final Logger logger = LoggerFactory.getLogger(StatsdLog.class);

    private String bucketPrepend = "";
    private double sampleRate = 1.0;
    private StatsdClient client;

    public String getStatsdBucketPrepend() {
        return bucketPrepend;
    }

    public void setStatsdBucketPrepend(String bucketPrepend) {

        if (bucketPrepend == null) {

            this.bucketPrepend = "";

        } else if ( ! "".equals(bucketPrepend) ) {

            if (bucketPrepend.contains("%HOSTNAME")) {

                String hostName;

                try {
                    hostName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    logger.warn("Could not get local hostname", e);
                    hostName = "";
                }

                this.bucketPrepend = bucketPrepend.replaceAll("%HOSTNAME", hostName);

            } else {

                this.bucketPrepend = bucketPrepend;

            }
        }
    }

    public Double getStatsdSampleRate() {
        return sampleRate;
    }

    public void setStatsdSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public StatsdClient getStatsdClient() {
        return client;
    }

    public void setStatsdServer(String hostAndPort) {
        try {

            if (hostAndPort == null || "".equals(hostAndPort)) {
                return;
            }

            String[] tokens = hostAndPort.split(":");

            InetAddress address = InetAddress.getByName(tokens[0]);
            int port = Integer.parseInt(tokens[1]);

            client = new StatsdClient(address, port);
        } catch (UnknownHostException e) {
            logger.error("unable to set address", e);
        } catch (IOException e) {
            logger.error("unable to init statsd client", e);
        }
    }

    @Override
    public void log(StopWatch sw) {
        super.log(sw);

        if (client == null) {
            return;
        }

        client.timing(bucketPrepend + sw.getTag() + ".elapsedTime", (int) sw.getElapsedTime(), sampleRate);
    }
}
