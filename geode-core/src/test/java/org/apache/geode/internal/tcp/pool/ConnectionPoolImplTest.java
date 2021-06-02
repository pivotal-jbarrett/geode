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

import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Relinquished;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.junit.Test;

import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.tcp.InternalConnection;
import org.apache.geode.internal.tcp.pool.ConnectionPoolImpl.ThreadChecked;

public class ConnectionPoolImplTest {

  @Test
  public void claimReturnsNullWhenEmpty() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl();
    final InternalDistributedMember member = mock(InternalDistributedMember.class);

    final PooledConnection connection = connectionPool.claim(member);

    assertThat(connection).isNull();
  }

  @Test
  public void claimReturnsNullForUnknownMember() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl();
    final InternalDistributedMember knownMember = mock(InternalDistributedMember.class);
    final InternalConnection connectionForKnownMember = mock(InternalConnection.class);
    when(connectionForKnownMember.getRemoteAddress()).thenReturn(knownMember);
    final InternalDistributedMember unknownMember = mock(InternalDistributedMember.class);
    connectionPool.relinquish(connectionPool.makePooled(connectionForKnownMember));
    clearInvocations(connectionForKnownMember);

    final PooledConnection connection = connectionPool.claim(unknownMember);

    assertThat(connection).isNull();
  }

  @Test
  public void claimReturnsConnectionForKnownMember() {
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl();
    connectionPool.relinquish(connectionPool.makePooled(connection));
    clearInvocations(connection);

    final PooledConnection pooledConnection = connectionPool.claim(member);

    assertThat(pooledConnection).isNotNull();
    assertThat(pooledConnection.getRemoteAddress()).isEqualTo(member);
  }

  @Test
  public void makePooledCreatesPooledConnectionAndCreatesPool() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl();
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);

    final PooledConnection pooledConnection = connectionPool.makePooled(connection);
    assertThat(pooledConnection).isNotNull();

    assertThat(connectionPool.pools).containsKey(member);
  }

  @Test
  public void makePooledWrapsWithThreadChecked() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, true);
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);

    final PooledConnection pooledConnection = connectionPool.makePooled(connection);

    assertThat(Proxy.getInvocationHandler(pooledConnection)).isNotNull()
        .isInstanceOf(ThreadChecked.class);
  }

  @Test
  public void pooledConnectionCanRelinquishViaSetInUseWhenThreadChecked() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, true);
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);

    final PooledConnection pooledConnection = connectionPool.makePooled(connection);
    pooledConnection.setInUse(true, 0, 0, 0, null);
    pooledConnection.setInUse(false, 0, 0, 0, null);

    verify(connection).setInUse(eq(true), eq(0), eq(0), eq(0), isNull());
    verify(connection).setInUse(eq(false), eq(0), eq(0), eq(0), isNull());
  }

  @Test
  public void claimWrapsWithThreadChecked() {
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, true);
    connectionPool.relinquish(connectionPool.makePooled(connection));
    clearInvocations(connection);

    final PooledConnection pooledConnection = connectionPool.claim(member);

    assertThat(pooledConnection).isNotNull();
    assertThat(Proxy.getInvocationHandler(pooledConnection)).isNotNull()
        .isInstanceOf(ThreadChecked.class);
  }

  @Test
  public void relinquishClosesConnectionIfNoPoolExists() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl();
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final InternalDistributedMember unknownMember = mock(InternalDistributedMember.class);
    when(pooledConnection.getRemoteAddress()).thenReturn(unknownMember);

    connectionPool.relinquish(pooledConnection);

    verify(pooledConnection).getRemoteAddress();
    verify(pooledConnection).closeOldConnection(any());
    verify(pooledConnection).setState(eq(Relinquished));
    verifyNoMoreInteractions(pooledConnection);
  }

  @Test
  public void relinquishClosesConnectionIfPoolIsFull() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(1);
    final InternalDistributedMember knownMember = mock(InternalDistributedMember.class);
    final InternalConnection connection1 = mock(InternalConnection.class);
    when(connection1.getRemoteAddress()).thenReturn(knownMember);
    final InternalConnection connection2 = mock(InternalConnection.class);
    when(connection2.getRemoteAddress()).thenReturn(knownMember);
    final PooledConnection pooledConnection1 = connectionPool.makePooled(connection1);
    final PooledConnection pooledConnection2 = connectionPool.makePooled(connection2);
    clearInvocations(connection1, connection2);

    connectionPool.relinquish(pooledConnection1);
    connectionPool.relinquish(pooledConnection2);

    verify(connection1).getRemoteAddress();
    verifyNoMoreInteractions(connection1);
    verify(connection2).getRemoteAddress();
    verify(connection2).closeOldConnection(any());
    verifyNoMoreInteractions(connection2);
  }

  @Test
  public void createPoolCreatesUnboundedPool() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl();
    final Deque<PooledConnection> pool = connectionPool.createPool();
    assertThat(pool).isNotInstanceOf(BlockingDeque.class);
  }

  @Test
  public void createPoolCreatesBoundedPool() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(1);
    final Deque<PooledConnection> pool = connectionPool.createPool();
    assertThat(pool).isInstanceOf(BlockingDeque.class);
    assertThat(((BlockingDeque<PooledConnection>) pool).remainingCapacity()).isEqualTo(1);
  }

  @Test
  public void threadCheckedWrapsAndUnwraps() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final PooledConnection threadChecked = ThreadChecked.wrap(pooledConnection);

    assertThat(Proxy.getInvocationHandler(threadChecked)).isNotNull()
        .isInstanceOf(ThreadChecked.class);
    assertThat(ThreadChecked.unwrap(threadChecked)).isSameAs(pooledConnection);
  }

  @Test
  public void threadCheckedThrowsWhenAccessedFromAnotherThread() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final PooledConnection threadChecked = ThreadChecked.wrap(pooledConnection);

    assertThatThrownBy(() -> Executors.newSingleThreadScheduledExecutor()
        .submit((Runnable) threadChecked::isSharedResource).get())
            .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalStateException.class);
  }

}
