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

package org.gridgain.grid.kernal.processors.dataload;

import org.gridgain.grid.*;
import org.gridgain.grid.dataload.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;

/**
 * Data loader future.
 *
 * @author @java.author
 * @version @java.version
 */
class GridDataLoaderFuture extends GridFutureAdapter<Object> {
    /** Data loader. */
    @GridToStringExclude
    private GridDataLoader dataLdr;

    /**
     * Default constructor for {@link Externalizable} support.
     */
    public GridDataLoaderFuture() {
        // No-op.
    }

    /**
     * @param ctx Context.
     * @param dataLdr Data loader.
     */
    GridDataLoaderFuture(GridKernalContext ctx, GridDataLoader dataLdr) {
        super(ctx);

        assert dataLdr != null;

        this.dataLdr = dataLdr;
    }

    /** {@inheritDoc} */
    @Override public boolean cancel() throws GridException {
        checkValid();

        if (onCancelled()) {
            dataLdr.close(true);

            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDataLoaderFuture.class, this, super.toString());
    }
}
