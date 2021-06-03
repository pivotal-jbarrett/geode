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

import static org.apache.geode.internal.tcp.pool.PooledConnection.State.Claimed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.junit.Test;

import org.apache.geode.distributed.internal.DirectReplyProcessor;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.SystemTimer;
import org.apache.geode.internal.serialization.KnownVersion;

public class ThreadCheckedPooledConnectionTest {
  @Test
  public void allMethodsDelegateToConnection() throws IOException {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    when(pooledConnection.isSharedResource()).thenReturn(true);
    final InternalDistributedMember member = mock(InternalDistributedMember.class);
    when(pooledConnection.getRemoteAddress()).thenReturn(member);
    when(pooledConnection.isStopped()).thenReturn(true);
    when(pooledConnection.isTimedOut()).thenReturn(true);
    when(pooledConnection.isConnected()).thenReturn(true);
    when(pooledConnection.getSendBufferSize()).thenReturn(42);
    when(pooledConnection.checkForIdleTimeout()).thenReturn(true);
    when(pooledConnection.isClosing()).thenReturn(true);
    when(pooledConnection.isSocketClosed()).thenReturn(true);
    when(pooledConnection.isReceiverStopped()).thenReturn(true);
    when(pooledConnection.getRemoteVersion()).thenReturn(KnownVersion.GEODE_1_12_1);
    when(pooledConnection.getOriginatedHere()).thenReturn(true);
    when(pooledConnection.getPreserveOrder()).thenReturn(true);
    when(pooledConnection.getUniqueId()).thenReturn(42L);
    when(pooledConnection.getMessagesReceived()).thenReturn(42L);
    when(pooledConnection.getMessagesSent()).thenReturn(42L);
    final ThreadCheckedPooledConnection threadChecked =
        new ThreadCheckedPooledConnection(pooledConnection);
    final DirectReplyProcessor directReplyProcessor = mock(DirectReplyProcessor.class);
    final SystemTimer.SystemTimerTask idleTimeoutTask = mock(SystemTimer.SystemTimerTask.class);
    final ByteBuffer preserialized =
        ByteBuffer.wrap("preserialized".getBytes(StandardCharsets.UTF_8));
    final DistributionMessage distributionMessage = mock(DistributionMessage.class);
    threadChecked.setState(Claimed);

    assertThat(threadChecked.isSharedResource()).isTrue();
    threadChecked.readAck(directReplyProcessor);
    assertThat(threadChecked.getRemoteAddress()).isEqualTo(member);
    threadChecked.setInUse(true, 1, 2, 3, null);
    assertThat(threadChecked.isStopped()).isTrue();
    assertThat(threadChecked.isTimedOut()).isTrue();
    assertThat(threadChecked.isConnected()).isTrue();
    assertThat(threadChecked.getSendBufferSize()).isEqualTo(42);
    threadChecked.setIdleTimeoutTask(idleTimeoutTask);
    assertThat(threadChecked.checkForIdleTimeout()).isTrue();
    threadChecked.cleanUpOnIdleTaskCancel();
    threadChecked.requestClose("requestClose");
    assertThat(threadChecked.isClosing()).isTrue();
    threadChecked.closePartialConnect("requestClose", true);
    threadChecked.closeForReconnect("closeForReconnect");
    threadChecked.closeOldConnection("closeOldConnection");
    threadChecked.sendPreserialized(preserialized, true, distributionMessage);
    threadChecked.scheduleAckTimeouts();
    assertThat(threadChecked.isSocketClosed()).isTrue();
    assertThat(threadChecked.isReceiverStopped()).isTrue();
    assertThat(threadChecked.getRemoteVersion()).isEqualTo(KnownVersion.GEODE_1_12_1);
    assertThat(threadChecked.getOriginatedHere()).isTrue();
    assertThat(threadChecked.getPreserveOrder()).isTrue();
    assertThat(threadChecked.getUniqueId()).isEqualTo(42);
    assertThat(threadChecked.getMessagesReceived()).isEqualTo(42);
    assertThat(threadChecked.getMessagesSent()).isEqualTo(42);
    threadChecked.extendAckSevereAlertTimeout();

    verify(pooledConnection).isSharedResource();
    verify(pooledConnection).readAck(eq(directReplyProcessor));
    verify(pooledConnection).getRemoteAddress();
    verify(pooledConnection).setInUse(eq(true), eq(1L), eq(2L), eq(3L), isNull());
    verify(pooledConnection).isStopped();
    verify(pooledConnection).isTimedOut();
    verify(pooledConnection).isConnected();
    verify(pooledConnection).getSendBufferSize();
    verify(pooledConnection).setIdleTimeoutTask(eq(idleTimeoutTask));
    verify(pooledConnection).checkForIdleTimeout();
    verify(pooledConnection).cleanUpOnIdleTaskCancel();
    verify(pooledConnection).requestClose(eq("requestClose"));
    verify(pooledConnection).isClosing();
    verify(pooledConnection).closePartialConnect(eq("requestClose"), eq(true));
    verify(pooledConnection).closeForReconnect(eq("closeForReconnect"));
    verify(pooledConnection).closeOldConnection(eq("closeOldConnection"));
    verify(pooledConnection).sendPreserialized(eq(preserialized), eq(true),
        eq(distributionMessage));
    verify(pooledConnection).scheduleAckTimeouts();
    verify(pooledConnection).isSocketClosed();
    verify(pooledConnection).isReceiverStopped();
    verify(pooledConnection).getOriginatedHere();
    verify(pooledConnection).getPreserveOrder();
    verify(pooledConnection).getUniqueId();
    verify(pooledConnection).getMessagesReceived();
    verify(pooledConnection).getMessagesSent();
    verify(pooledConnection).extendAckSevereAlertTimeout();
  }

  @Test
  public void throwsWhenAccessedFromAnotherThread() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ThreadCheckedPooledConnection threadCheckedPooledConnection =
        new ThreadCheckedPooledConnection(pooledConnection);

    assertThatThrownBy(() -> Executors.newSingleThreadScheduledExecutor()
        .submit((Runnable) threadCheckedPooledConnection::isSharedResource).get())
            .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalStateException.class);

    verifyNoInteractions(pooledConnection);
  }

  @Test
  public void getDelegateReturnsOriginalPooledConnection() {
    final PooledConnection pooledConnection = mock(PooledConnection.class);
    final ThreadCheckedPooledConnection threadCheckedPooledConnection =
        new ThreadCheckedPooledConnection(pooledConnection);

    assertThat(threadCheckedPooledConnection.getDelegate()).isSameAs(pooledConnection);
  }
}
