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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Deque;
import java.util.concurrent.BlockingDeque;

import org.junit.Test;

import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.tcp.InternalConnection;

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

    assertThat(pooledConnection).isNotNull().isInstanceOf(ThreadCheckedPooledConnection.class);
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

    verify(connection).setInUse(eq(true), eq(0L), eq(0L), eq(0L), isNull());
    verify(connection).setInUse(eq(false), eq(0L), eq(0L), eq(0L), isNull());
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

    assertThat(pooledConnection).isNotNull().isInstanceOf(ThreadCheckedPooledConnection.class);
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
  public void removeIfExistsRemovesFromPool() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(1);
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);
    final PooledConnection pooledConnection = connectionPool.makePooled(connection);

    connectionPool.relinquish(pooledConnection);
    connectionPool.removeIfExists(pooledConnection);

    assertThat(connectionPool.claim(member)).isNull();
  }

  @Test
  public void removeIfExistsIgnoreMissingPool() {
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(1);
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.getRemoteAddress()).thenReturn(member);
    final PooledConnection pooledConnection = connectionPool.makePooled(connection);

    connectionPool.removeIfExists(pooledConnection);
  }

  @Test
  public void wrapThreadCheckedWrapsWhenEnabled() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, true);
    final PooledConnection threadChecked = connectionPool.wrapThreadChecked(pooledConnection);

    assertThat(threadChecked).isNotNull().isInstanceOf(ThreadCheckedPooledConnection.class);
    assertThat(((ThreadCheckedPooledConnection) threadChecked).getDelegate())
        .isSameAs(pooledConnection);
  }

  @Test
  public void wrapThreadCheckedDoesNotWrapWhenDisabled() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, false);
    final PooledConnection wrapped = connectionPool.wrapThreadChecked(pooledConnection);

    assertThat(wrapped).isSameAs(pooledConnection);
  }

  @Test
  public void unwrapThreadCheckedWhenEnabled() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, true);
    final PooledConnection threadChecked = connectionPool.wrapThreadChecked(pooledConnection);
    final PooledConnection unwrapThreadChecked = connectionPool.unwrapThreadChecked(threadChecked);

    assertThat(unwrapThreadChecked).isSameAs(pooledConnection);
  }

  @Test
  public void unwrapThreadCheckedDoesNotUnwrapWhenEnableButConnectionIsNotThreadChecked() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, true);
    final PooledConnection unwrapThreadChecked =
        connectionPool.unwrapThreadChecked(pooledConnection);

    assertThat(unwrapThreadChecked).isSameAs(pooledConnection);
  }

  @Test
  public void unwrapThreadCheckedDoesNotUnwrapWhenDisabled() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ConnectionPoolImpl connectionPool = new ConnectionPoolImpl(0, false);
    final PooledConnection unwrapThreadChecked =
        connectionPool.unwrapThreadChecked(pooledConnection);

    assertThat(unwrapThreadChecked).isSameAs(pooledConnection);
  }

}
