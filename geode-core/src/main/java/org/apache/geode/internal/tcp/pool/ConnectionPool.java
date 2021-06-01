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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.tcp.InternalConnection;

public interface ConnectionPool {

  /**
   * Makes the provided connection pooled and creates a pool for the remote member if one does not
   * already exist.
   *
   * @param connection to make pooled.
   * @return pooled connection
   */
  @NotNull
  PooledConnection makePooled(@NotNull InternalConnection connection);

  /**
   * Claim a pooled connection for the given distributedMember.
   *
   * @param distributedMember to claim a pooled connection for.
   * @return an existing pooled connection for the distributedMember if one is available, otherwise
   *         {@code null}.
   */
  @Nullable
  PooledConnection claim(@NotNull InternalDistributedMember distributedMember);

  /**
   * Relinquishes a previously claimed pooled connection. If a pool no longer exists for the remote
   * member then this connection may be closed. If the pool is bounded and if adding this connection
   * to the pool would exceed that bound then it may be closed.
   *
   * @param pooledConnection to relinquish
   */
  void relinquish(@NotNull PooledConnection pooledConnection);

}
