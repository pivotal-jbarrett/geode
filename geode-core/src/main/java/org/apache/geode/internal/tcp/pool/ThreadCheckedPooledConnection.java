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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.geode.distributed.internal.DirectReplyProcessor;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.SystemTimer.SystemTimerTask;
import org.apache.geode.internal.serialization.KnownVersion;
import org.apache.geode.internal.tcp.Connection;
import org.apache.geode.internal.tcp.ConnectionException;

public final class ThreadCheckedPooledConnection implements PooledConnection {
  private final PooledConnection delegate;
  private final Thread owner;

  public ThreadCheckedPooledConnection(final @NotNull PooledConnection delegate) {
    this.delegate = delegate;

    owner = currentThread();
  }

  public @NotNull PooledConnection getDelegate() {
    return delegate;
  }

  private void checkThread() {
    if (owner != currentThread()) {
      throw new IllegalStateException(
          format("Attempt to invoke method from %s while owned by %s", currentThread(),
              owner));
    }
  }

  @Override
  public boolean isSharedResource() {
    checkThread();
    return delegate.isSharedResource();
  }

  @Override
  public void readAck(
      final @NotNull DirectReplyProcessor processor)
      throws SocketTimeoutException, ConnectionException {
    checkThread();
    delegate.readAck(processor);
  }

  @Override
  public @Nullable InternalDistributedMember getRemoteAddress() {
    checkThread();
    return delegate.getRemoteAddress();
  }

  @Override
  public void setInUse(final boolean use, final long startTime, final long ackWaitThreshold,
      final long ackSAThreshold,
      final @Nullable List<@NotNull Connection> connectionGroup) {
    checkThread();
    delegate.setInUse(use, startTime, ackWaitThreshold, ackSAThreshold, connectionGroup);
  }

  @Override
  public boolean isStopped() {
    checkThread();
    return delegate.isStopped();
  }

  @Override
  public boolean isTimedOut() {
    checkThread();
    return delegate.isTimedOut();
  }

  @Override
  public boolean isConnected() {
    checkThread();
    return delegate.isConnected();
  }

  @Override
  public int getSendBufferSize() {
    checkThread();
    return delegate.getSendBufferSize();
  }

  @Override
  public void setIdleTimeoutTask(
      final @NotNull SystemTimerTask task) {
    checkThread();
    delegate.setIdleTimeoutTask(task);
  }

  @Override
  public boolean checkForIdleTimeout() {
    checkThread();
    return delegate.checkForIdleTimeout();
  }

  @Override
  public void cleanUpOnIdleTaskCancel() {
    checkThread();
    delegate.cleanUpOnIdleTaskCancel();
  }

  @Override
  public void requestClose(@NotNull final String reason) {
    checkThread();
    delegate.requestClose(reason);
  }

  @Override
  public boolean isClosing() {
    checkThread();
    return delegate.isClosing();
  }

  @Override
  public void closePartialConnect(@NotNull final String reason, final boolean beingSick) {
    checkThread();
    delegate.closePartialConnect(reason, beingSick);
  }

  @Override
  public void closeForReconnect(@NotNull final String reason) {
    checkThread();
    delegate.closeForReconnect(reason);
  }

  @Override
  public void closeOldConnection(@NotNull final String reason) {
    checkThread();
    delegate.closeOldConnection(reason);
  }

  @Override
  public void sendPreserialized(final @NotNull ByteBuffer buffer, final boolean cacheContentChanges,
      final @Nullable DistributionMessage msg)
      throws IOException, ConnectionException {
    checkThread();
    delegate.sendPreserialized(buffer, cacheContentChanges, msg);
  }

  @Override
  public void scheduleAckTimeouts() {
    checkThread();
    delegate.scheduleAckTimeouts();
  }

  @Override
  public boolean isSocketClosed() {
    checkThread();
    return delegate.isSocketClosed();
  }

  @Override
  public boolean isReceiverStopped() {
    checkThread();
    return delegate.isReceiverStopped();
  }

  @Override
  public @Nullable KnownVersion getRemoteVersion() {
    checkThread();
    return delegate.getRemoteVersion();
  }

  @Override
  public boolean getOriginatedHere() {
    checkThread();
    return delegate.getOriginatedHere();
  }

  @Override
  public boolean getPreserveOrder() {
    checkThread();
    return delegate.getPreserveOrder();
  }

  @Override
  public long getUniqueId() {
    checkThread();
    return delegate.getUniqueId();
  }

  @Override
  public long getMessagesReceived() {
    checkThread();
    return delegate.getMessagesReceived();
  }

  @Override
  public long getMessagesSent() {
    checkThread();
    return delegate.getMessagesSent();
  }

  @Override
  public void extendAckSevereAlertTimeout() {
    checkThread();
    delegate.extendAckSevereAlertTimeout();
  }

  @Override
  public void setState(final State state) {
    checkThread();
    delegate.setState(state);
  }

}
