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

import static org.apache.geode.internal.lang.utils.JavaWorkarounds.computeIfAbsent;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Claimed;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Relinquished;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.geode.annotations.VisibleForTesting;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.tcp.InternalConnection;
import org.apache.geode.logging.internal.log4j.api.LogService;

public class ConnectionPoolImpl implements ConnectionPool {
  private static final Logger log = LogService.getLogger();

  @VisibleForTesting
  final ConcurrentMap<InternalDistributedMember, Deque<PooledConnection>> pools =
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
  public @Nullable PooledConnection claim(
      @NotNull final InternalDistributedMember distributedMember) {
    final Deque<PooledConnection> pool = pools.get(distributedMember);
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
    computeIfAbsent(pools, connection.getRemoteAddress(), (distributedMember) -> {
      log.info("Creating new connection pool for member {}.", distributedMember);
      return createPool();
    });

    final PooledConnectionImpl pooledConnection = new PooledConnectionImpl(this, connection);
    log.info("Pooled connection {} created for.", pooledConnection);

    return claim(pooledConnection);
  }

  private @NotNull PooledConnection claim(final @NotNull PooledConnection pooledConnection) {
    pooledConnection.setState(Claimed);

    return wrapThreadChecked(pooledConnection);
  }

  @Override
  public void relinquish(@NotNull final PooledConnection pooledConnection) {
    pooledConnection.setState(Relinquished);

    final InternalDistributedMember distributedMember = pooledConnection.getRemoteAddress();
    final Deque<PooledConnection> pool = pools.get(distributedMember);
    if (null == pool) {
      log.info("Pooled connection {} relinquished but no pool for member {} exists.",
          pooledConnection,
          distributedMember);
      pooledConnection.closeOldConnection("No connection pool for member.");
      return;
    }

    if (!pool
        .offerFirst(unwrapThreadChecked(pooledConnection))) {
      log.info("Pooled connection {} relinquished but pool for member {} rejected it.",
          pooledConnection, distributedMember);
      pooledConnection.closeOldConnection("Pool rejected connection.");
    }

    log.debug("Pooled connection {} relinquished.", pooledConnection);

    // TODO increment stats
  }

  @VisibleForTesting
  @NotNull
  PooledConnection wrapThreadChecked(final @NotNull PooledConnection pooledConnection) {
    return useThreadChecked ? new ThreadCheckedPooledConnection(pooledConnection)
        : pooledConnection;
  }

  @VisibleForTesting
  @NotNull
  PooledConnection unwrapThreadChecked(final @NotNull PooledConnection pooledConnection) {
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
