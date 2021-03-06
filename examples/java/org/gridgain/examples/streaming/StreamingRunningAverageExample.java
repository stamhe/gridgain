/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
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

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.streaming;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.streamer.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.product.GridProductEdition.*;

/**
 * Example to demonstrate how to compute a running average. In this example
 * random numbers are being streamed into the system and the streamer
 * continuously maintains a running average over last {@code 500} numbers.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-streaming.xml'}.
 * <p>
 * Alternatively you can run {@link StreamingNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-streaming.xml} configuration.
 *
 * @author @java.author
 * @version @java.version
 */
@GridOnlyAvailableIn(STREAMING)
public class StreamingRunningAverageExample {
    /**
     * Main method.
     *
     * @param args Parameters.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        Grid grid = GridGain.start("examples/config/example-streamer.xml");

        System.out.println();
        System.out.println(">>> Streaming running average example started.");

        final GridStreamer streamer = grid.streamer("running-average");

        final int rndRange = 100;

        // This thread executes a query across all nodes
        // to collect a running average from all of them.
        // During reduce step the results are collected
        // and reduced into one average value.
        Thread qryThread = new Thread(new Runnable() {
            @SuppressWarnings("BusyWait")
            @Override public void run() {
                while (!Thread.interrupted()) {
                    try {
                        try {
                            Thread.sleep(3000);
                        }
                        catch (InterruptedException ignore) {
                            return;
                        }

                        // Running average.
                        double avg = streamer.context().reduce(
                            new GridClosure<GridStreamerContext, Average>() {
                                @Override public Average apply(GridStreamerContext ctx) {
                                    return ctx.<String, Average>localSpace().get("avg");
                                }
                            },
                            new GridReducer<Average, Double>() {
                                private Average avg = new Average();

                                @Override public boolean collect(@Nullable Average a) {
                                    if (a != null)
                                        avg.add(a);

                                    return true;
                                }

                                @Override public Double reduce() {
                                    return avg.average();
                                }
                            }
                        );

                        System.out.println("Got streamer query result [avg=" + avg + ", idealAvg=" + (rndRange / 2) + ']');
                    }
                    catch (GridException e) {
                        System.out.println("Failed to execute streamer query: " + e);
                    }
                }
            }
        });

        // This thread continuously stream events
        // into the system.
        Thread evtThread = new Thread(new Runnable() {
            @Override public void run() {
                Random rnd = new Random();

                while (!Thread.interrupted()) {
                    try {
                        streamer.addEvent(rnd.nextInt(rndRange));
                    }
                    catch (GridException e) {
                        System.out.println("Failed to add streamer event: " + e);
                    }
                }
            }
        });

        try {
            System.out.println(">>> Starting streamer query and producer threads. Press enter to stop this example.");

            qryThread.start();
            evtThread.start();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                in.readLine();
            }
        }
        finally {
            qryThread.interrupt();
            evtThread.interrupt();

            qryThread.join();
            evtThread.join();

            GridGain.stopAll(false);
        }
    }

    /**
     * Sample streamer stage to compute average.
     */
    public static class StreamerStage implements GridStreamerStage<Integer> {
        /** {@inheritDoc} */
        @Override public String name() {
            return "exampleStage";
        }

        /** {@inheritDoc} */
        @Nullable @Override public Map<String, Collection<?>> run(GridStreamerContext ctx, Collection<Integer> evts)
            throws GridException {
            ConcurrentMap<String, Average> loc = ctx.localSpace();

            Average avg = loc.get("avg");

            // Store average in local space if it was not done before.
            if (avg == null) {
                Average old = loc.putIfAbsent("avg", avg = new Average());

                if (old != null)
                    avg = old;
            }

            // For every input event, update the average.
            for (Integer e : evts)
                avg.add(e, 1);

            GridStreamerWindow<Integer> win = ctx.window();

            // Add input events to window.
            win.enqueueAll(evts);

            while (true) {
                Integer e = win.pollEvicted();

                if (e == null)
                    break;

                // Subtract evicted events from running average.
                avg.add(-e, -1);
            }

            return null;
        }
    }

    /**
     * Class to help calculate average.
     */
    public static class Average {
        /** */
        private int total;

        /** */
        private int cnt;

        /**
         * Adds one average to another.
         *
         * @param avg Average to add.
         */
        public void add(Average avg) {
            int total;
            int cnt;

            synchronized (avg) {
                total = avg.total;
                cnt = avg.cnt;
            }

            add(total, cnt);
        }

        /**
         * Adds passed in values to current values.
         * <p>
         * Note that this method is synchronized because multiple
         * threads will be updating the same average instance concurrently.
         *
         * @param total Total delta.
         * @param cnt Count delta.
         */
        public synchronized void add(int total, int cnt) {
            this.total += total;
            this.cnt += cnt;
        }

        /**
         * Calculates current average based on total value and count.
         * <p>
         * Note that this method is synchronized because multiple
         * threads will be updating the same average instance concurrently.

         * @return Running average.
         */
        public synchronized double average() {
            return (double)total / cnt;
        }
    }
}
