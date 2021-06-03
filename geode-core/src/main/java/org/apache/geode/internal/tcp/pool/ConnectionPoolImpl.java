/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.internal.tcp.pool;

import static java.util.Objects.requireNonNull;
import static org.apache.geode.internal.lang.utils.JavaWorkarounds.computeIfAbsent;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Claimed;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Relinquished;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Removed;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.geode.annotations.VisibleForTesting;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.tcp.InternalConnection;
import org.apache.geode.logging.internal.log4j.api.LogService;

public class ConnectionPoolImpl implements ConnectionPool {
  private static final Logger log = LogService.getLogger();

  /**
   * All connections available in this pool.
   */
  @VisibleForTesting final ConcurrentMap<DistributedMember, Deque<PooledConnection>> pools =
      new ConcurrentHashMap<>();

  /**
   * All connections associated with this pool. Includes currently claimed connections.
   */
  private final ConcurrentMap<DistributedMember, Set<PooledConnection>> connections =
      new ConcurrentHashMap<>();

  private final int capacity;

  private final boolean useThreadChecked;

  /**
   * Construct an unbounded member connection pool.
   */
  public ConnectionPoolImpl() {
    this(0);
  }

  /**
   * Construct a bounded member connection pool.
   *
   * @param capacity to limit each member pool too.
   */
  public ConnectionPoolImpl(final int capacity) {
    this(capacity, false);
  }

  ConnectionPoolImpl(final int capacity, final boolean useThreadChecked) {
    this.capacity = capacity;
    this.useThreadChecked = useThreadChecked;
  }

  @Override
  public @Nullable PooledConnection claim(@NotNull final DistributedMember distributedMember) {
    final Deque<PooledConnection> pool = getPool(distributedMember);
    if (null == pool) {
      log.info("No connection pool for member {}.", distributedMember);
      return null;
    }

    final PooledConnection pooledConnection = pool.pollFirst();
    if (log.isDebugEnabled()) {
      if (null == pooledConnection) {
        log.debug("Pooled connection pool empty for member {}.", distributedMember);
      } else {
        log.debug("Pooled connection {} claimed.", pooledConnection);
      }
    }

    if (null == pooledConnection) {
      return null;
    }

    return claim(pooledConnection);
  }

  @Override
  public @NotNull PooledConnection makePooled(final @NotNull InternalConnection connection) {
    final InternalDistributedMember distributedMember = connection.getRemoteAddress();
    computeIfAbsent(pools, distributedMember, (k) -> {
      log.info("Creating new connection pool for member {}.", k);
      return createPool();
    });

    final PooledConnectionImpl pooledConnection = new PooledConnectionImpl(this, connection);
    log.info("Pooled connection {} created.", pooledConnection);

    put(pooledConnection);

    return claim(pooledConnection);
  }

  @Override
  public boolean contains(@NotNull final DistributedMember distributedMember) {
    final Set<PooledConnection> pooledConnections = getPooledConnections(distributedMember);
    return !(null == pooledConnections || pooledConnections.isEmpty());
  }

  @Override
  public @Nullable InternalDistributedMember closeAll(@NotNull final DistributedMember distributedMember, @NotNull Consumer<InternalConnection> closer) {
    final Deque<PooledConnection> pool = getPool(distributedMember);
    if (null != pool) {
      pool.clear();
    }
    InternalDistributedMember remoteAddress = null;
    final Set<PooledConnection> pooledConnections = getPooledConnections(distributedMember);
    if (null != pooledConnections) {
      for (Iterator<PooledConnection> iterator = pooledConnections.iterator();
           iterator.hasNext(); ) {
        final PooledConnection pooledConnection = iterator.next();
        iterator.remove();
        remoteAddress = null == remoteAddress ? pooledConnection.getRemoteAddress() : remoteAddress;
        closer.accept(pooledConnection);
      }
    }
    return remoteAddress;
  }

  private void put(final @NotNull PooledConnection pooledConnection) {
    computeIfAbsent(connections, pooledConnection.getRemoteAddress(),
        (k) -> ConcurrentHashMap.newKeySet()).add(pooledConnection);
  }

  private void remove(final @NotNull PooledConnection pooledConnection) {
    final Set<PooledConnection> pooledConnections = getPooledConnections(pooledConnection);
    if (null != pooledConnections) {
      pooledConnections.remove(pooledConnection);
    }
  }

  private @NotNull PooledConnection claim(final @NotNull PooledConnection pooledConnection) {
    pooledConnection.setState(Claimed);
    return wrapThreadChecked(pooledConnection);
  }

  @Override
  public void relinquish(@NotNull final PooledConnection pooledConnection) {
    pooledConnection.setState(Relinquished);

    final Deque<PooledConnection> pool = getPool(pooledConnection);
    if (null == pool) {
      log.info("Pooled connection {} relinquished but no pool for member exists.",
          pooledConnection);
      pooledConnection.closeOldConnection("No connection pool for member.");
      return;
    }

    if (!pool
        .offerFirst(unwrapThreadChecked(pooledConnection))) {
      log.info("Pooled connection {} relinquished but pool for member rejected it.",
          pooledConnection);
      try {
        pooledConnection.closeOldConnection("Pool rejected connection.");
      } finally {
        remove(pooledConnection);
      }
    }

    log.debug("Pooled connection {} relinquished.", pooledConnection);

    // TODO increment stats
  }

  @Override
  public void removeIfExists(@NotNull final PooledConnection pooledConnection) {
    pooledConnection.setState(Removed);

    final Deque<PooledConnection> pool = getPool(pooledConnection);
    if (null == pool) {
      log.info("Pooled connection {} removed but no pool for member exists.",
          pooledConnection);
      return;
    }

    // Idle connections should be at the end of the queue.
    if (pool.removeLastOccurrence(unwrapThreadChecked(pooledConnection))) {
      log.info("ConnectionPool.removeIfExists: removed {}.", pooledConnection);
    }

    remove(pooledConnection);
  }

  private @Nullable Deque<PooledConnection> getPool(final @NotNull DistributedMember distributedMember) {
    return pools.get(distributedMember);
  }

  private @Nullable Deque<PooledConnection> getPool(
      final @NotNull PooledConnection pooledConnection) {
    return getPool(requireNonNull(pooledConnection.getRemoteAddress()));
  }

  private Set<PooledConnection> getPooledConnections(
      final @NotNull DistributedMember distributedMember) {
    return connections.get(distributedMember);
  }

  private @Nullable Set<PooledConnection> getPooledConnections(
      final @NotNull PooledConnection pooledConnection) {
    return getPooledConnections(requireNonNull(pooledConnection.getRemoteAddress()));
  }

  @VisibleForTesting
  @NotNull
  PooledConnection wrapThreadChecked(final @NotNull PooledConnection pooledConnection) {
    return useThreadChecked ? new ThreadCheckedPooledConnection(pooledConnection)
        : pooledConnection;
  }

  @NotNull
  public PooledConnection unwrapThreadChecked(final @NotNull PooledConnection pooledConnection) {
    if (useThreadChecked && pooledConnection instanceof ThreadCheckedPooledConnection) {
      return ((ThreadCheckedPooledConnection) pooledConnection).getDelegate();
    }
    return pooledConnection;
  }

  @VisibleForTesting
  @NotNull
  Deque<PooledConnection> createPool() {
    return (capacity > 0) ? new LinkedBlockingDeque<>(capacity) : new ConcurrentLinkedDeque<>();
  }

  // TODO idle connection cleanup
  // TODO stats: pools, connections per pool, added, idle, removed, claims/releases?
}
