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

package org.gridgain.examples.messaging;

import org.gridgain.examples.*;
import org.gridgain.examples.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;

import java.util.*;

/**
 * Example that demonstrates how to exchange messages between nodes. Use such
 * functionality for cases when you need to communicate to other nodes outside
 * of grid task.
 * <p>
 * To run this example you must have at least one remote node started.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link ComputeNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-compute.xml} configuration.
 *
 * @author @java.author
 * @version @java.version
 */
public final class MessagingExample {
    /** Message topics. */
    private enum TOPIC { ORDERED, UNORDERED }

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid g = GridGain.start("examples/config/example-compute.xml")) {
            if (!ExamplesUtils.checkMinTopologySize(g, 2))
                return;

            System.out.println();
            System.out.println(">>> Messaging example started.");

            // Projection for remote nodes.
            GridProjection rmtPrj = g.forRemotes();

            // Register listeners on all grid nodes.
            startListening(rmtPrj);

            // Send unordered messages to all remote nodes.
            for (int i = 0; i < 10; i++)
                rmtPrj.message().send(TOPIC.UNORDERED, Integer.toString(i));

            System.out.println(">>> Finished sending unordered messages.");

            // Send ordered messages to all remote nodes.
            for (int i = 0; i < 10; i++)
                rmtPrj.message().sendOrdered(TOPIC.ORDERED, Integer.toString(i), 0);

            System.out.println(">>> Finished sending ordered messages.");
            System.out.println(">>> Check output on all nodes for message printouts.");
        }
    }

    /**
     * Start listening to messages on all grid nodes within passed in projection.
     *
     * @param prj Grid projection.
     * @throws GridException If failed.
     */
    private static void startListening(GridProjection prj) throws GridException {
        // Add ordered message listener.
        prj.message().remoteListen(TOPIC.ORDERED, new GridBiPredicate<UUID, String>() {
            @Override public boolean apply(UUID nodeId, String msg) {
                System.out.println("Received ordered message [msg=" + msg + ", fromNodeId=" + nodeId + ']');

                return true; // Return true to continue listening.
            }
        }).get();

        // Add unordered message listener.
        prj.message().remoteListen(TOPIC.UNORDERED, new GridBiPredicate<UUID, String>() {
            @Override public boolean apply(UUID nodeId, String msg) {
                System.out.println("Received unordered message [msg=" + msg + ", fromNodeId=" + nodeId + ']');

                return true; // Return true to continue listening.
            }
        }).get();
    }
}
