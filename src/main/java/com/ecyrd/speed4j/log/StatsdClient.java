package com.ecyrd.speed4j.log;

/**
 * StatsdClient.java
 *
 * (C) 2011 Meetup, Inc.
 * Author: Andrew Gwozdziewycz <andrew@meetup.com>, @apgwoz
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:

 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Example usage:
 *
 *    StatsdClient client = new StatsdClient("statsd.example.com", 8125);
 *    // increment by 1
 *    client.increment("foo.bar.baz");
 *    // increment by 10
 *    client.increment("foo.bar.baz", 10);
 *    // sample rate
 *    client.increment("foo.bar.baz", 10, .1);
 *    // increment multiple keys by 1
 *    client.increment("foo.bar.baz", "foo.bar.boo", "foo.baz.bar");
 *    // increment multiple keys by 10 -- yeah, it's "backwards"
 *    client.increment(10, "foo.bar.baz", "foo.bar.boo", "foo.baz.bar");
 *    // multiple keys with a sample rate
 *    client.increment(10, .1, "foo.bar.baz", "foo.bar.boo", "foo.baz.bar");
 *
 * Note: For best results, and greater availability, you'll probably want to
 * create a wrapper class which creates a static client and proxies to it.
 *
 * You know... the "Java way."
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsdClient {
    private static final Random RNG = new Random();
    private static final Logger log = LoggerFactory.getLogger(StatsdClient.class);

    private InetSocketAddress _address;
    private DatagramChannel _channel;

    public StatsdClient(String host, int port) throws UnknownHostException, IOException {
        this(InetAddress.getByName(host), port);
    }

    public StatsdClient(InetAddress host, int port) throws IOException {
        _address = new InetSocketAddress(host, port);
        _channel = DatagramChannel.open();
    }

    public boolean timing(String key, int value) {
        return timing(key, value, 1.0);
    }

    public boolean timing(String key, int value, double sampleRate) {
        return send(sampleRate, String.format("%s:%d|ms", key, value));
    }

    public boolean decrement(String key) {
        return increment(key, -1, 1.0);
    }

    public boolean decrement(String key, int magnitude) {
        return decrement(key, magnitude, 1.0);
    }

    public boolean decrement(String key, int magnitude, double sampleRate) {
        magnitude = magnitude < 0 ? magnitude : -magnitude;
        return increment(key, magnitude, sampleRate);
    }

    public boolean decrement(String... keys) {
        return increment(-1, 1.0, keys);
    }

    public boolean decrement(int magnitude, String... keys) {
        magnitude = magnitude < 0 ? magnitude : -magnitude;
        return increment(magnitude, 1.0, keys);
    }

    public boolean decrement(int magnitude, double sampleRate, String... keys) {
        magnitude = magnitude < 0 ? magnitude : -magnitude;
        return increment(magnitude, sampleRate, keys);
    }

    public boolean increment(String key) {
        return increment(key, 1, 1.0);
    }

    public boolean increment(String key, int magnitude) {
        return increment(key, magnitude, 1.0);
    }

    public boolean increment(String key, int magnitude, double sampleRate) {
        String stat = String.format("%s:%s|c", key, magnitude);
        return send(stat, sampleRate);
    }

    public boolean increment(int magnitude, double sampleRate, String... keys) {
        String[] stats = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            stats[i] = String.format("%s:%s|c", keys[i], magnitude);
        }
        return send(sampleRate, stats);
    }

    private boolean send(String stat, double sampleRate) {
        return send(sampleRate, stat);
    }

    private boolean send(double sampleRate, String... stats) {

        boolean retval = false; // didn't send anything
        if (sampleRate < 1.0) {
            for (String stat : stats) {
                if (RNG.nextDouble() <= sampleRate) {
                    stat = String.format("%s|@%f", stat, sampleRate);
                    if (doSend(stat)) {
                        retval = true;
                    }
                }
            }
        } else {
            for (String stat : stats) {
                if (doSend(stat)) {
                    retval = true;
                }
            }
        }

        return retval;
    }

    private boolean doSend(final String stat) {
        try {
            final byte[] data = stat.getBytes("utf-8");
            final ByteBuffer buff = ByteBuffer.wrap(data);
            final int nbSentBytes = _channel.send(buff, _address);

            if (data.length == nbSentBytes) {
                return true;
            } else {
                log.error(String.format(
                        "Could not send entirely stat %s to host %s:%d. Only sent %d bytes out of %d bytes", stat,
                        _address.getHostName(), _address.getPort(), nbSentBytes, data.length));
                return false;
            }

        } catch (IOException e) {
            log.error(
                    String.format("Could not send stat %s to host %s:%d", stat, _address.getHostName(),
                            _address.getPort()), e);
            return false;
        }
    }
}
