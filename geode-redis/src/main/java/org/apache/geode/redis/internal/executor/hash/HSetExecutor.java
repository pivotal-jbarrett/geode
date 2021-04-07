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

import static io.netty.buffer.Unpooled.copiedBuffer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

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

  static final Map<ByteBuf, Map<ByteBuf, byte[]>> data = new HashMap<>();

  @Override
  public RedisResponse executeCommand(Command command,
      ExecutionHandlerContext context) {
    throw new IllegalStateException();
  }

  @Override
  public ByteBuf executeCommand2(final Command command, final ExecutionHandlerContext context) {
    final List<ByteBuf> commandElems = command.getCommandParts();
    final int size = commandElems.size();

    final ByteBuf key = commandElems.get(1);

    Map<ByteBuf, byte[]> hash = data.get(key);

    if (null == hash) {
      hash = new HashMap<>((size-2) / 2);
      data.put(copiedBuffer(key), hash);
    }

    for (int i = 2; i < size;) {
      final ByteBuf k = commandElems.get(i++);
      final byte[] v = copiedArray(commandElems.get(i++));
      if (null != hash.replace(k, v)) {
        hash.put(copiedBuffer(k), v);
      }
    }

    // TODO correct return value
    final ByteBuf buffer = context.getByteBufAllocator().buffer();
    return buffer.writeByte(Coder.INTEGER_ID).writeByte(48).writeBytes(Coder.CRLFar);
  }

  private static byte[] copiedArray(final ByteBuf buffer) {
    final byte[] bytes = new byte[buffer.readableBytes()];
    buffer.getBytes(0, bytes);
    return bytes;
  }

  protected boolean onlySetOnAbsent() {
    return false;
  }
}
