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

package org.apache.geode.internal.tcp;

import static org.apache.geode.util.internal.UncheckedUtils.uncheckedCast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.CancelCriterion;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.DMStats;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.distributed.internal.membership.api.Membership;
import org.apache.geode.internal.tcp.pool.ConnectionPool;
import org.apache.geode.internal.tcp.pool.PooledConnection;
import org.apache.geode.test.junit.categories.MembershipTest;

@Category(MembershipTest.class)
public class ConnectionTableTest {

  private ConnectionTable connectionTable;
  private Socket socket;
  private PeerConnectionFactory factory;
  private InternalConnection connection;
  private ConnectionPool connectionPool;
  private TCPConduit tcpConduit;

  @Before
  public void initConnectionTable() throws Exception {
    InternalDistributedSystem system = mock(InternalDistributedSystem.class);
    when(system.isShareSockets()).thenReturn(false);

    DistributionManager dm = mock(DistributionManager.class);
    when(dm.getSystem()).thenReturn(system);

    CancelCriterion cancelCriterion = mock(CancelCriterion.class);
    DMStats dmStats = mock(DMStats.class);

    tcpConduit = mock(TCPConduit.class);
    when(tcpConduit.getDM()).thenReturn(dm);
    when(tcpConduit.getCancelCriterion()).thenReturn(cancelCriterion);
    when(tcpConduit.getStats()).thenReturn(dmStats);

    connection = mock(InternalConnection.class);

    socket = mock(Socket.class);

    connectionPool = mock(ConnectionPool.class);

    connectionTable = new ConnectionTable(tcpConduit, connectionPool);

    factory = mock(PeerConnectionFactory.class);
    when(factory.createReceiver(connectionTable, socket)).thenReturn(connection);
  }

  @Test
  public void testConnectionsClosedDuringCreateAreNotAddedAsReceivers() throws Exception {
    when(connection.isReceiverStopped()).thenReturn(false);
    // Pretend this closed as soon at it was created
    when(connection.isSocketClosed()).thenReturn(true);

    connectionTable.acceptConnection(socket, factory);
    assertEquals(0, connectionTable.getNumberOfReceivers());
  }

  @Test
  public void testThreadStoppedNotAddedAsReceivers() throws Exception {
    // connection is not closed
    when(connection.isSocketClosed()).thenReturn(false);

    // but receiver is stopped
    when(connection.isReceiverStopped()).thenReturn(true);

    connectionTable.acceptConnection(socket, factory);
    assertEquals(0, connectionTable.getNumberOfReceivers());
  }

  @Test
  public void testSocketNotClosedAddedAsReceivers() throws Exception {
    // connection is not closed
    when(connection.isSocketClosed()).thenReturn(false);

    connectionTable.acceptConnection(socket, factory);
    assertEquals(1, connectionTable.getNumberOfReceivers());
  }

  @Test
  public void testThreadOwnedSocketsAreRemoved() {
    final Boolean wantsResources = ConnectionTable.getThreadOwnsResourcesRegistration();
    ConnectionTable.threadWantsOwnResources();
    try {
      Map<DistributedMember, InternalConnection> threadConnectionMap = new HashMap<>();
      ConnectionTable.threadOrderedConnMap.set(threadConnectionMap);
      ConnectionTable.releaseThreadsSockets();
      assertEquals(0, threadConnectionMap.size());
    } finally {
      if (wantsResources != Boolean.FALSE) {
        ConnectionTable.threadWantsSharedResources();
      }
    }
  }

  @Test
  @Ignore("Needs to be integration test or mock out some connection factory.")
  public void getPooledConnectionReturnsConnectionForUnknownMember() throws IOException {
    final Membership<InternalDistributedMember> membership = uncheckedCast(mock(Membership.class));
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    when(membership.memberExists(eq(member))).thenReturn(true);
    when(tcpConduit.getMembership()).thenReturn(membership);
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    when(connectionPool.makePooled(any())).thenReturn(pooledConnection);

    final InternalConnection connection =
        connectionTable.getPooledConnection(member, 0, 0, 0);

    assertThat(connection).isNotNull();
    assertThat(connection).isInstanceOf(PooledConnection.class);

    verify(connectionPool).claim(eq(member));
    verify(connectionPool).makePooled(any());
    verifyNoMoreInteractions(connectionPool);
  }

  @Test
  public void getPooledConnectionReturnsConnectionForKnownMember() throws IOException {
    final InternalDistributedMember knownMember = mock(InternalDistributedMember.class);
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    when(connectionPool.claim(eq(knownMember))).thenReturn(pooledConnection);

    final InternalConnection connection = connectionTable.getPooledConnection(knownMember, 0, 0, 0);

    assertThat(connection).isEqualTo(pooledConnection);

    verify(connectionPool).claim(eq(knownMember));
    verifyNoMoreInteractions(connectionPool);
  }

}
