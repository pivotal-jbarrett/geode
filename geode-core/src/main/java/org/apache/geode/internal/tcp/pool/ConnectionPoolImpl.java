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

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.apache.geode.internal.lang.utils.JavaWorkarounds.computeIfAbsent;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Claimed;
import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Relinquished;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
    this(capacity, true);
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

    return useThreadChecked ? ThreadChecked.wrap(pooledConnection) : pooledConnection;
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
        .offerFirst(useThreadChecked ? ThreadChecked.unwrap(pooledConnection) : pooledConnection)) {
      log.info("Pooled connection {} relinquished but pool for member {} rejected it.",
          pooledConnection, distributedMember);
      pooledConnection.closeOldConnection("Pool rejected connection.");
    }

    log.debug("Pooled connection {} relinquished.", pooledConnection);

    // TODO increment stats
  }

  @VisibleForTesting
  @NotNull
  Deque<PooledConnection> createPool() {
    return (capacity > 0) ? new LinkedBlockingDeque<>(capacity) : new ConcurrentLinkedDeque<>();
  }

  static class ThreadChecked implements InvocationHandler {
    private @NotNull final PooledConnection pooledConnection;
    private @NotNull final Thread owner;

    public ThreadChecked(final @NotNull PooledConnection pooledConnection) {
      this.pooledConnection = pooledConnection;

      owner = currentThread();
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable {
      if (owner != currentThread()) {
        throw new IllegalStateException(
            format("Attempt to invoke method from %s while owned by %s", currentThread(),
                owner));
      }
      try {
        return method.invoke(pooledConnection, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }

    @NotNull
    PooledConnection unwrap() {
      return pooledConnection;
    }

    static @NotNull PooledConnection wrap(final @NotNull PooledConnection pooledConnection) {
      return (PooledConnection) Proxy.newProxyInstance(ConnectionPoolImpl.class.getClassLoader(),
          new Class<?>[] {PooledConnection.class},
          new ThreadChecked(pooledConnection));
    }

    static @NotNull PooledConnection unwrap(final @NotNull PooledConnection pooledConnection) {
      if (Proxy.isProxyClass(pooledConnection.getClass())) {
        return ((ThreadChecked) Proxy.getInvocationHandler(pooledConnection)).unwrap();
      }
      return pooledConnection;
    }

  }

  // TODO idle connection cleanup
  // TODO stats: pools, connections per pool, added, idle, removed, claims/releases?
}
