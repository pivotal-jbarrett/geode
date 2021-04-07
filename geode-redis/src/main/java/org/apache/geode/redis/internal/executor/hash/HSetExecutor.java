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
package org.apache.geode.redis.internal.executor.hash;

import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;

import org.apache.geode.redis.internal.executor.RedisResponse;
import org.apache.geode.redis.internal.netty.Coder;
import org.apache.geode.redis.internal.netty.Command;
import org.apache.geode.redis.internal.netty.ExecutionHandlerContext;

/**
 * <pre>
 * Implements the HSET command to sets field in the hash stored at key to value.
 * A new entry in the hash is created if key does not exist.
 * Any existing in the hash with the given key is overwritten.
 *
 * Examples:
 *
 * redis> HSET myhash field1 "Hello"
 * (integer) 1
 * redis> HGET myhash field1
 *
 *
 * </pre>
 */
public class HSetExecutor extends HashExecutor {

  static final Map<byte[], Map<byte[], byte[]>> data = new Object2ObjectOpenCustomHashMap<>(ByteArrays.HASH_STRATEGY);

  @Override
  public RedisResponse executeCommand(Command command,
      ExecutionHandlerContext context) {
    throw new IllegalStateException();
  }

  @Override
  public ByteBuf executeCommand2(final Command command, final ExecutionHandlerContext context) {
    // TODO keep as ByteBuf
    final List<byte[]> commandElems = command.getProcessedCommand();
    final int size = commandElems.size();

    final byte[] key = commandElems.get(1);

    Map<byte[], byte[]> hash = data.get(key);

    if (null == hash) {
      hash = new Object2ObjectOpenCustomHashMap<>((size-2) / 2, ByteArrays.HASH_STRATEGY);
      data.put(key, hash);
    }

    for (int i = 2; i < size;) {
      hash.put(commandElems.get(i++), commandElems.get(i++));
    }

    // TODO correct return value
    final ByteBuf buffer = context.getByteBufAllocator().buffer();
    return buffer.writeByte(Coder.INTEGER_ID).writeByte(48).writeBytes(Coder.CRLFar);
  }

  protected boolean onlySetOnAbsent() {
    return false;
  }
}
