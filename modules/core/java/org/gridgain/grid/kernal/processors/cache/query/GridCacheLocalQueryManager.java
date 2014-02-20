// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.query;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.indexing.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Local query manager.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridCacheLocalQueryManager<K, V> extends GridCacheQueryManager<K, V> {
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected boolean onPageReady(
        boolean loc,
        GridCacheQueryInfo<K, V> qryInfo,
        Collection<?> data,
        boolean finished, Throwable e) {
        GridCacheQueryFutureAdapter fut = qryInfo.localQueryFuture();

        assert fut != null;

        if (e != null)
            fut.onPage(null, null, e, true);
        else
            fut.onPage(null, data, null, finished);

        return true;
    }

    /** {@inheritDoc} */
    @Override protected boolean onFieldsPageReady(boolean loc,
        GridCacheQueryInfo<K, V> qryInfo,
        @Nullable List<GridCacheQueryFieldDescriptor> metaData,
        @Nullable Collection<List<GridIndexingEntity<?>>> entities,
        @Nullable Collection<?> data,
        boolean finished,
        @Nullable Throwable e) {
        assert qryInfo != null;

        GridCacheLocalFieldsQueryFuture fut = (GridCacheLocalFieldsQueryFuture)qryInfo.localQueryFuture();

        assert fut != null;

        if (e != null)
            fut.onPage(null, null, null, e, true);
        else
            fut.onPage(null, metaData, data, null, finished);

        return true;
    }

    /** {@inheritDoc} */
    @Override public void start0() throws GridException {
        super.start0();

        assert cctx.config().getCacheMode() == LOCAL;
    }

    /** {@inheritDoc} */
    @Override public <R> GridCacheQueryFuture<R> queryLocal(GridCacheQueryBaseAdapter<K, V> qry, boolean single,
        boolean rmtRdcOnly, @Nullable GridBiInClosure<UUID, Collection<R>> pageLsnr, @Nullable GridPredicate<?> vis) {
        return queryLocal(qry, single, rmtRdcOnly, pageLsnr, vis, false);
    }

    /** {@inheritDoc} */
    @Override public <R> Collection<R> queryLocalSync(GridCacheQueryBaseAdapter<K, V> qry, boolean single,
        boolean rmtRdcOnly, @Nullable GridBiInClosure<UUID, Collection<R>> pageLsnr, @Nullable GridPredicate<?> vis)
        throws GridException {
        GridCacheQueryFuture<R> fut = queryLocal(qry, single, rmtRdcOnly, pageLsnr, vis, true);

        assert fut.isDone();

        return fut.get();
    }

    /**
     * @param qry Query.
     * @param single {@code true} if single result requested, {@code false} if multiple.
     * @param rmtRdcOnly {@code true} for reduce query when using remote reducer only,
     *      otherwise it is always {@code false}.
     * @param pageLsnr Page listener.
     * @param vis Visitor Predicate.
     * @param sync Whether to execute synchronously.
     * @return Iterator over query results. Note that results become available as they come.
     */
    private <R> GridCacheQueryFuture<R> queryLocal(GridCacheQueryBaseAdapter<K, V> qry, boolean single,
        boolean rmtRdcOnly, @Nullable GridBiInClosure<UUID, Collection<R>> pageLsnr, @Nullable GridPredicate<?> vis,
        boolean sync) {
        assert cctx.config().getCacheMode() == LOCAL;

        if (log.isDebugEnabled())
            log.debug("Executing query on local node: " + qry);

        GridCacheLocalQueryFuture<K, V, R> fut =
            new GridCacheLocalQueryFuture<>(cctx, qry, single, rmtRdcOnly, pageLsnr, vis);

        try {
            validateQuery(qry);

            fut.execute(sync);
        }
        catch (GridException e) {
            fut.onDone(e);
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override public <R> GridCacheQueryFuture<R> queryDistributed(GridCacheQueryBaseAdapter<K, V> qry,
        Collection<GridNode> nodes, boolean single, boolean rmtOnly,
        @Nullable GridBiInClosure<UUID, Collection<R>> pageLsnr, @Nullable GridPredicate<?> vis) {
        assert cctx.config().getCacheMode() == LOCAL;

        throw new GridRuntimeException("Distributed queries are not available for local cache " +
            "(use 'GridCacheQuery.execute(grid.localNode())' instead) [cacheName=" + cctx.name() + ']');
    }

    /** {@inheritDoc} */
    @Override public void loadPage(long id, GridCacheQueryBaseAdapter<K, V> qry,
        Collection<GridNode> nodes, boolean all) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public GridCacheFieldsQueryFuture queryFieldsLocal(GridCacheFieldsQueryAdapter qry, boolean single,
        boolean rmtOnly, @Nullable GridBiInClosure<UUID, Collection<List<Object>>> pageLsnr,
        @Nullable GridPredicate<?> vis) {
        return queryFieldsLocal(qry, single, rmtOnly, pageLsnr, vis, false);
    }

    /** {@inheritDoc} */
    @Override public Collection<List<Object>> queryFieldsLocalSync(GridCacheFieldsQueryAdapter qry, boolean single,
        boolean rmtOnly, @Nullable GridBiInClosure<UUID, Collection<List<Object>>> pageLsnr,
        @Nullable GridPredicate<?> vis) throws GridException {
        GridCacheFieldsQueryFuture fut = queryFieldsLocal(qry, single, rmtOnly, pageLsnr, vis, true);

        assert fut.isDone();

        return fut.get();
    }

    /**
     * @param qry Query.
     * @param single {@code true} if single result requested, {@code false} if multiple.
     * @param rmtOnly {@code true} for reduce query when using remote reducer only,
     *      otherwise it is always {@code false}.
     * @param pageLsnr Page listener.
     * @param vis Visitor predicate.
     * @param sync Whether to execute synchronously.
     * @return Iterator over query results. Note that results become available as they come.
     */
    private GridCacheFieldsQueryFuture queryFieldsLocal(GridCacheFieldsQueryAdapter qry, boolean single,
        boolean rmtOnly, @Nullable GridBiInClosure<UUID, Collection<List<Object>>> pageLsnr,
        @Nullable GridPredicate<?> vis, boolean sync) {
        assert cctx.config().getCacheMode() == LOCAL;

        if (log.isDebugEnabled())
            log.debug("Executing query on local node: " + qry);

        GridCacheLocalFieldsQueryFuture fut = new GridCacheLocalFieldsQueryFuture(
            cctx, qry, single, rmtOnly, pageLsnr, vis);

        try {
            validateQuery(qry);

            fut.execute(sync);
        }
        catch (GridException e) {
            fut.onDone(e);
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override public GridCacheFieldsQueryFuture queryFieldsDistributed(GridCacheFieldsQueryAdapter qry,
        Collection<GridNode> nodes, boolean single, boolean rmtOnly,
        @Nullable GridBiInClosure<UUID, Collection<List<Object>>> pageLsnr, @Nullable GridPredicate<?> vis) {
        assert cctx.config().getCacheMode() == LOCAL;

        throw new GridRuntimeException("Distributed queries are not available for local cache " +
            "(use 'GridCacheQuery.execute(grid.localNode())' instead) [cacheName=" + cctx.name() + ']');
    }
}