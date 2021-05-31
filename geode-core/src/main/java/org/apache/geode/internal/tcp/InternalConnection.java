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

import static java.lang.String.format;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.geode.annotations.VisibleForTesting;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.internal.SystemTimer;
import org.apache.geode.internal.serialization.KnownVersion;

public interface InternalConnection extends Connection {
  /**
   * string used as the reason for initiating suspect processing
   */
  @VisibleForTesting
  String INITIATING_SUSPECT_PROCESSING =
      "member unexpectedly shut down shared, unordered connection";
  int MSG_HEADER_SIZE_OFFSET = 0;
  int MSG_HEADER_TYPE_OFFSET = 4;
  int MSG_HEADER_ID_OFFSET = 5;
  int MSG_HEADER_BYTES = 7;
  int NORMAL_MSG_TYPE = 0x4c;
  int CHUNKED_MSG_TYPE = 0x4d; // a chunk of one logical msg
  int END_CHUNKED_MSG_TYPE = 0x4e; // last in a series of chunks
  int DIRECT_ACK_BIT = 0x20;

  static int calcHdrSize(int byteSize) {
    if (byteSize > MAX_MSG_SIZE) {
      throw new IllegalStateException(format("tcp message exceeded max size of %s",
          MAX_MSG_SIZE));
    }
    int hdrSize = byteSize;
    hdrSize |= ConnectionImpl.HANDSHAKE_VERSION << 24;
    return hdrSize;
  }

  boolean isStopped();

  boolean isTimedOut();

  boolean isConnected();

  /**
   * Returns the size of the send buffer on this connection's socket.
   */
  int getSendBufferSize();

  void setIdleTimeoutTask(SystemTimer.SystemTimerTask task);

  /**
   * Returns true if an idle connection was detected.
   */
  boolean checkForIdleTimeout();

  void cleanUpOnIdleTaskCancel();

  /**
   * Request that the manager close this connection, or close it forcibly if there is no manager.
   * Invoking this method ensures that the proper synchronization is done.
   */
  void requestClose(String reason);

  boolean isClosing();

  void closePartialConnect(String reason, boolean beingSick);

  void closeForReconnect(String reason);

  void closeOldConnection(String reason);

  /**
   * sends a serialized message to the other end of this connection. This is used by the
   * DirectChannel in GemFire when the message is going to be sent to multiple recipients.
   *
   * @throws ConnectionException if the conduit has stopped
   */
  void sendPreserialized(@NotNull ByteBuffer buffer, boolean cacheContentChanges,
      @Nullable DistributionMessage msg) throws IOException, ConnectionException;

  /**
   * ensure that a task is running to monitor transmission and reading of acks
   */
  void scheduleAckTimeouts();

  boolean isSocketClosed();

  boolean isReceiverStopped();

  /**
   * Return the version of the member on the other side of this connection.
   */
  KnownVersion getRemoteVersion();

  /**
   * answers whether this connection was initiated in this vm
   *
   * @return true if the connection was initiated here
   * @since GemFire 5.1
   */
  boolean getOriginatedHere();

  /**
   * answers whether this connection is used for ordered message delivery
   */
  boolean getPreserveOrder();

  /**
   * answers the unique ID of this connection in the originating VM
   */
  long getUniqueId();

  /**
   * answers the number of messages received by this connection
   */
  long getMessagesReceived();

  /**
   * answers the number of messages sent on this connection
   */
  long getMessagesSent();
}
