// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.typedef;

import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.lang.*;

/**
 * Defines {@code alias} for <tt>GridPredicate2&lt;K, V&gt;</tt> by extending
 * {@link GridPredicate}. Since Java doesn't provide type aliases (like Scala, for example) we resort
 * to these types of measures. This is intended to provide for more concise code without sacrificing
 * readability. For more information see {@link GridPredicate}.
 *
 * @author @java.author
 * @version @java.version
 * @see GridBiPredicate
 * @see GridFunc
 */
public abstract class PKV<K, V> extends GridBiPredicate<K, V> { /* No-op. */ }