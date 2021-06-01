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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import org.apache.geode.distributed.internal.DirectReplyProcessor;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.SystemTimer;
import org.apache.geode.internal.serialization.KnownVersion;
import org.apache.geode.internal.tcp.InternalConnection;

public class PooledConnectionImplTest {

  @Test
  public void allMethodsDelegateToConnection() throws IOException {
    final ConnectionPool connectionPool = mock(ConnectionPool.class);
    final InternalConnection connection = mock(InternalConnection.class);
    when(connection.isSharedResource()).thenReturn(true);
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    when(connection.getRemoteAddress()).thenReturn(member);
    when(connection.isStopped()).thenReturn(true);
    when(connection.isTimedOut()).thenReturn(true);
    when(connection.isConnected()).thenReturn(true);
    when(connection.getSendBufferSize()).thenReturn(42);
    when(connection.checkForIdleTimeout()).thenReturn(true);
    when(connection.isClosing()).thenReturn(true);
    when(connection.isSocketClosed()).thenReturn(true);
    when(connection.isReceiverStopped()).thenReturn(true);
    when(connection.getRemoteVersion()).thenReturn(KnownVersion.GEODE_1_12_1);
    when(connection.getOriginatedHere()).thenReturn(true);
    when(connection.getPreserveOrder()).thenReturn(true);
    when(connection.getUniqueId()).thenReturn(42L);
    when(connection.getMessagesReceived()).thenReturn(42L);
    when(connection.getMessagesSent()).thenReturn(42L);
    final PooledConnection pooledConnection = new PooledConnectionImpl(connectionPool, connection);
    final DirectReplyProcessor directReplyProcessor = mock(DirectReplyProcessor.class);
    final SystemTimer.SystemTimerTask idleTimeoutTask = mock(SystemTimer.SystemTimerTask.class);
    final ByteBuffer preserialized =
        ByteBuffer.wrap("preserialized".getBytes(StandardCharsets.UTF_8));
    final DistributionMessage distributionMessage = mock(
        DistributionMessage.class);

    assertThat(pooledConnection.isSharedResource()).isTrue();
    pooledConnection.readAck(directReplyProcessor);
    assertThat(pooledConnection.getRemoteAddress()).isEqualTo(member);
    pooledConnection.setInUse(true, 1, 2, 3, null);
    assertThat(pooledConnection.isStopped()).isTrue();
    assertThat(pooledConnection.isTimedOut()).isTrue();
    assertThat(pooledConnection.isConnected()).isTrue();
    assertThat(pooledConnection.getSendBufferSize()).isEqualTo(42);
    pooledConnection.setIdleTimeoutTask(idleTimeoutTask);
    assertThat(pooledConnection.checkForIdleTimeout()).isTrue();
    pooledConnection.cleanUpOnIdleTaskCancel();
    pooledConnection.requestClose("requestClose");
    assertThat(pooledConnection.isClosing()).isTrue();
    pooledConnection.closePartialConnect("requestClose", true);
    pooledConnection.closeForReconnect("closeForReconnect");
    pooledConnection.closeOldConnection("closeOldConnection");
    pooledConnection.sendPreserialized(preserialized, true, distributionMessage);
    pooledConnection.scheduleAckTimeouts();
    assertThat(pooledConnection.isSocketClosed()).isTrue();
    assertThat(pooledConnection.isReceiverStopped()).isTrue();
    assertThat(pooledConnection.getRemoteVersion()).isEqualTo(KnownVersion.GEODE_1_12_1);
    assertThat(pooledConnection.getOriginatedHere()).isTrue();
    assertThat(pooledConnection.getPreserveOrder()).isTrue();
    assertThat(pooledConnection.getUniqueId()).isEqualTo(42);
    assertThat(pooledConnection.getMessagesReceived()).isEqualTo(42);
    assertThat(pooledConnection.getMessagesSent()).isEqualTo(42);
    pooledConnection.extendAckSevereAlertTimeout();

    verify(connection).isSharedResource();
    verify(connection).readAck(eq(directReplyProcessor));
    verify(connection).getRemoteAddress();
    verify(connection).setInUse(eq(true), eq(1L), eq(2L), eq(3L), isNull());
    verify(connection).isStopped();
    verify(connection).isTimedOut();
    verify(connection).isConnected();
    verify(connection).getSendBufferSize();
    verify(connection).setIdleTimeoutTask(eq(idleTimeoutTask));
    verify(connection).checkForIdleTimeout();
    verify(connection).cleanUpOnIdleTaskCancel();
    verify(connection).requestClose(eq("requestClose"));
    verify(connection).isClosing();
    verify(connection).closePartialConnect(eq("requestClose"), eq(true));
    verify(connection).closeForReconnect(eq("closeForReconnect"));
    verify(connection).closeOldConnection(eq("closeOldConnection"));
    verify(connection).sendPreserialized(eq(preserialized), eq(true), eq(distributionMessage));
    verify(connection).scheduleAckTimeouts();
    verify(connection).isSocketClosed();
    verify(connection).isReceiverStopped();
    verify(connection).getOriginatedHere();
    verify(connection).getPreserveOrder();
    verify(connection).getUniqueId();
    verify(connection).getMessagesReceived();
    verify(connection).getMessagesSent();
    verify(connection).extendAckSevereAlertTimeout();
  }

  @Test
  public void setInUseRelinquishesToPoolWhenInUseIsFalse() {
    final ConnectionPool connectionPool = mock(ConnectionPool.class);
    final InternalConnection connection = mock(InternalConnection.class);
    final PooledConnection pooledConnection = new PooledConnectionImpl(connectionPool, connection);

    pooledConnection.setInUse(false, 0, 0, 0, null);

    verify(connection).setInUse(eq(false), eq(0L), eq(0L), eq(0L), isNull());
    verify(connectionPool).relinquish(eq(pooledConnection));

    verifyNoMoreInteractions(connection, connectionPool);
  }

  @Test
  public void setInUseDoesNotRelinquishToPoolWhenInUseIsTrue() {
    final ConnectionPool connectionPool = mock(ConnectionPool.class);
    final InternalConnection connection = mock(InternalConnection.class);
    final PooledConnection pooledConnection = new PooledConnectionImpl(connectionPool, connection);

    pooledConnection.setInUse(true, 0, 0, 0, null);

    verify(connection).setInUse(eq(true), eq(0L), eq(0L), eq(0L), isNull());

    verifyNoMoreInteractions(connection, connectionPool);
  }
}
