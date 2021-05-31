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


import java.net.SocketTimeoutException;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.geode.distributed.internal.DirectReplyProcessor;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;

public interface Connection {

  /**
   * maximum message buffer size
   */
  int MAX_MSG_SIZE = 0x00ffffff;

  String THREAD_KIND_IDENTIFIER = "P2P message reader";

  boolean isSharedResource();

  /**
   * @throws SocketTimeoutException if wait expires.
   * @throws ConnectionException if ack is not received
   */
  void readAck(@NotNull DirectReplyProcessor processor)
      throws SocketTimeoutException, ConnectionException;

  /**
   * return the DM id of the member on the other side of this connection.
   */
  @Nullable
  InternalDistributedMember getRemoteAddress();

  /**
   * If {@code use} is true then "claim" the connection for our use. If {@code use} is
   * false then "release" the connection.
   */
  void setInUse(boolean use, long startTime, long ackWaitThreshold, long ackSAThreshold,
      @Nullable List<@NotNull Connection> connectionGroup);
}
