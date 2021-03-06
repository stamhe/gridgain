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

/*
 * ___    _________________________ ________
 * __ |  / /____  _/__  ___/__  __ \___  __ \
 * __ | / /  __  /  _____ \ _  / / /__  /_/ /
 * __ |/ /  __/ /   ____/ / / /_/ / _  _, _/
 * _____/   /___/   /____/  \____/  /_/ |_|
 *
 */
package org.gridgain.visor

import org.scalatest._
import org.scalatest.matchers._
import org.gridgain.grid._
import org.gridgain.grid.{GridGain => G}

/**
 * Base abstract class for unit tests requiring Visor runtime.
 *
 * @author @java.author
 * @version @java.version
 */
abstract class VisorRuntimeBaseSpec(private[this] val num: Int) extends FlatSpec with ShouldMatchers
    with BeforeAndAfterAll {
    assert(num >= 1)

    /**
     * Runs before all tests.
     */
    final override def beforeAll() {
        (1 to num).foreach((n: Int) => GridGain.start(config("node-" + n)))
    }

    /**
     * Runs after all tests.
     */
    override def afterAll() {
        (1 to num).foreach((n: Int) => GridGain.stop("node-" + n, false))
    }

    /**
     * Gets grid configuration.
     *
     * @param name Grid name.
     * @return Grid configuration.
     */
    protected def config(name: String): GridConfiguration = {
        val cfg = new GridConfiguration

        cfg.setGridName(name)

        cfg
    }
}
