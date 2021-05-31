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

import static org.apache.geode.internal.inet.LocalHostUtil.getLocalHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.DataInput;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.CancelCriterion;
import org.apache.geode.alerting.internal.spi.AlertingAction;
import org.apache.geode.distributed.internal.DMStats;
import org.apache.geode.distributed.internal.Distribution;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.internal.monitoring.ThreadsMonitoring;
import org.apache.geode.internal.monitoring.executor.AbstractExecutor;
import org.apache.geode.internal.net.BufferPool;
import org.apache.geode.internal.net.SocketCloser;
import org.apache.geode.test.junit.categories.MembershipTest;

@Category(MembershipTest.class)
public class ConnectionImplTest {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void canBeMocked() throws Exception {
    ConnectionImpl mockConnection = mock(ConnectionImpl.class);
    SocketChannel channel = null;
    ByteBuffer buffer = null;
    boolean forceAsync = true;
    DistributionMessage mockDistributionMessage = mock(DistributionMessage.class);

    mockConnection.writeFully(channel, buffer, forceAsync, mockDistributionMessage);

    verify(mockConnection, times(1)).writeFully(channel, buffer, forceAsync,
        mockDistributionMessage);
  }

  /**
   * Test whether suspicion is raised about a member that closes its shared/unordered TCPConduit
   * connection
   */
  @Test
  public void testSuspicionRaised() throws Exception {
    ConnectionTable connectionTable = mock(ConnectionTable.class);
    Distribution distribution = mock(Distribution.class);
    DistributionManager distributionManager = mock(DistributionManager.class);
    DMStats dmStats = mock(DMStats.class);
    CancelCriterion stopper = mock(CancelCriterion.class);
    SocketCloser socketCloser = mock(SocketCloser.class);
    TCPConduit tcpConduit = mock(TCPConduit.class);
    ThreadsMonitoring threadMonitoring = mock(ThreadsMonitoring.class);
    AbstractExecutor abstractExecutor = mock(AbstractExecutor.class);

    when(connectionTable.getBufferPool()).thenReturn(new BufferPool(dmStats));
    when(connectionTable.getConduit()).thenReturn(tcpConduit);
    when(connectionTable.getDM()).thenReturn(distributionManager);
    when(connectionTable.getSocketCloser()).thenReturn(socketCloser);
    when(distributionManager.getDistribution()).thenReturn(distribution);
    when(stopper.cancelInProgress()).thenReturn(null);
    when(tcpConduit.getCancelCriterion()).thenReturn(stopper);
    when(tcpConduit.getDM()).thenReturn(distributionManager);
    when(tcpConduit.getSocketId()).thenReturn(new InetSocketAddress(getLocalHost(), 10337));
    when(tcpConduit.getStats()).thenReturn(dmStats);
    when(distributionManager.getThreadMonitoring()).thenReturn(threadMonitoring);
    when(threadMonitoring.createAbstractExecutor(any())).thenReturn(abstractExecutor);

    SocketChannel channel = SocketChannel.open();

    ConnectionImpl connection = new ConnectionImpl(connectionTable, channel.socket());
    connection.setSharedUnorderedForTest();
    connection.run();

    verify(distribution).suspectMember(isNull(), anyString());
  }

  @Test
  public void connectTimeoutIsShortWhenAlerting() throws UnknownHostException {
    ConnectionTable connectionTable = mock(ConnectionTable.class);
    DistributionConfig distributionConfig = mock(DistributionConfig.class);
    TCPConduit tcpConduit = mock(TCPConduit.class);

    when(connectionTable.getConduit()).thenReturn(tcpConduit);
    when(connectionTable.getBufferPool()).thenReturn(mock(BufferPool.class));
    when(distributionConfig.getMemberTimeout()).thenReturn(100);
    when(tcpConduit.getSocketId()).thenReturn(new InetSocketAddress(getLocalHost(), 12345));

    ConnectionImpl connection = new ConnectionImpl(connectionTable, mock(Socket.class));

    int normalTimeout = connection.getP2PConnectTimeout(distributionConfig);
    assertThat(normalTimeout).isEqualTo(600);

    AlertingAction.execute(
        () -> assertThat(connection.getP2PConnectTimeout(distributionConfig)).isEqualTo(100));
  }

  private ConnectionImpl createSpiedConnection() throws IOException {
    ConnectionTable connectionTable = mock(ConnectionTable.class);
    Distribution distribution = mock(Distribution.class);
    DistributionManager distributionManager = mock(DistributionManager.class);
    DMStats dmStats = mock(DMStats.class);
    CancelCriterion stopper = mock(CancelCriterion.class);
    SocketCloser socketCloser = mock(SocketCloser.class);
    TCPConduit tcpConduit = mock(TCPConduit.class);

    when(connectionTable.getBufferPool()).thenReturn(new BufferPool(dmStats));
    when(connectionTable.getConduit()).thenReturn(tcpConduit);
    when(connectionTable.getDM()).thenReturn(distributionManager);
    when(connectionTable.getSocketCloser()).thenReturn(socketCloser);
    when(distributionManager.getDistribution()).thenReturn(distribution);
    when(stopper.cancelInProgress()).thenReturn(null);
    when(tcpConduit.getCancelCriterion()).thenReturn(stopper);
    when(tcpConduit.getDM()).thenReturn(distributionManager);
    when(tcpConduit.getSocketId()).thenReturn(new InetSocketAddress(getLocalHost(), 10337));
    when(tcpConduit.getStats()).thenReturn(dmStats);

    SocketChannel channel = SocketChannel.open();

    ConnectionImpl connection = new ConnectionImpl(connectionTable, channel.socket());
    connection = spy(connection);
    return connection;
  }

  @Test
  public void firstCallToNotifyHandshakeWaiterWillClearSSLInputBuffer() throws Exception {
    ConnectionImpl connection = createSpiedConnection();
    connection.notifyHandshakeWaiter(true);
    verify(connection, times(1)).clearSSLInputBuffer();
  }

  @Test
  public void secondCallWithTrueToNotifyHandshakeWaiterShouldNotClearSSLInputBuffer()
      throws Exception {
    ConnectionImpl connection = createSpiedConnection();
    connection.notifyHandshakeWaiter(true);
    connection.notifyHandshakeWaiter(true);
    verify(connection, times(1)).clearSSLInputBuffer();
  }

  @Test
  public void secondCallWithFalseToNotifyHandshakeWaiterShouldNotClearSSLInputBuffer()
      throws Exception {
    ConnectionImpl connection = createSpiedConnection();
    connection.notifyHandshakeWaiter(true);
    connection.notifyHandshakeWaiter(false);
    verify(connection, times(1)).clearSSLInputBuffer();
  }

  @Test
  public void checkHandshakeInitialByteAccepts0() throws IOException {
    final DataInput dataInput = mock(DataInput.class);
    when(dataInput.readByte()).thenReturn((byte) 0);

    ConnectionImpl.checkHandshakeInitialByte(dataInput);

    verify(dataInput).readByte();
    verifyNoMoreInteractions(dataInput);
  }

  @Test
  public void checkHandshakeInitialByteThrowsNot0() throws IOException {
    final DataInput dataInput = mock(DataInput.class);
    when(dataInput.readByte()).thenReturn((byte) 1);

    assertThatThrownBy(() -> ConnectionImpl.checkHandshakeInitialByte(dataInput))
        .isInstanceOf(IllegalStateException.class);

    verify(dataInput).readByte();
    verifyNoMoreInteractions(dataInput);
  }

  @Test
  public void checkHandshakeVersionAcceptsCurrentVersion() throws IOException {
    final DataInput dataInput = mock(DataInput.class);
    when(dataInput.readByte()).thenReturn(ConnectionImpl.HANDSHAKE_VERSION);

    ConnectionImpl.checkHandshakeVersion(dataInput);

    verify(dataInput).readByte();
    verifyNoMoreInteractions(dataInput);
  }

  @Test
  public void checkHandshakeVersionThrowsWhenNotCurrentVersion() throws IOException {
    final DataInput dataInput = mock(DataInput.class);
    when(dataInput.readByte()).thenReturn((byte) 1);

    assertThatThrownBy(() -> ConnectionImpl.checkHandshakeVersion(dataInput))
        .isInstanceOf(IllegalStateException.class);

    verify(dataInput).readByte();
    verifyNoMoreInteractions(dataInput);
  }

  @Test
  public void readDominoNumberSharedResourceTrueReturns0() throws IOException {
    final DataInput dataInput = mock(DataInput.class);
    when(dataInput.readInt()).thenReturn(1);

    final int dominoNumber = ConnectionImpl.readDominoNumber(dataInput, true);
    assertThat(dominoNumber).isEqualTo(0);

    verify(dataInput).readInt();
    verifyNoMoreInteractions(dataInput);
  }

  @Test
  public void readDominoNumberSharedResourceFalseReturnsValue() throws IOException {
    final DataInput dataInput = mock(DataInput.class);
    when(dataInput.readInt()).thenReturn(1);

    final int dominoNumber = ConnectionImpl.readDominoNumber(dataInput, false);
    assertThat(dominoNumber).isEqualTo(1);

    verify(dataInput).readInt();
    verifyNoMoreInteractions(dataInput);
  }

}
