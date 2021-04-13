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

import static org.apache.geode.redis.internal.executor.hash.DummyCache.cache;
import static org.apache.geode.redis.internal.netty.Coder.CRLFar;
import static org.apache.geode.redis.internal.netty.Coder.INTEGER_ID;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import org.apache.geode.redis.internal.executor.RedisResponse;
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

    Map<ByteBuf, ByteBuf> hash = cache.get(key);

    if (null == hash) {
      hash = new HashMap<>((size-2) / 2);
      cache.put(copiedBuffer(key).asReadOnly(), hash);
    }

    int addedFields = 0;
    for (int i = 2; i < size;) {
      final ByteBuf k = commandElems.get(i++);
      final ByteBuf v = copiedBuffer(commandElems.get(i++)).asReadOnly();
      if (null == hash.replace(k, v)) {
        hash.put(copiedBuffer(k).asReadOnly(), v);
        addedFields++;
      }
    }

    return toRespInteger(addedFields, context.getByteBufAllocator());
  }

  static ByteBuf copiedBuffer(final ByteBuf buffer) {
//    return Unpooled.copiedBuffer(buffer);
    final int readableBytes = buffer.readableBytes();
    if (readableBytes > 0) {
      final ByteBuf copy = Unpooled.directBuffer(readableBytes, readableBytes);
      copy.writeBytes(buffer, buffer.readerIndex(), readableBytes);
      return copy;
    }

    return Unpooled.EMPTY_BUFFER;
  }

  static final int RESP_INTEGER_CAPACITY = 1 /*:*/ + 11 /*int*/ + 2 /*crlf*/;

  static ByteBuf toRespInteger(final int value, final ByteBufAllocator byteBufAllocator) {
    final ByteBuf buffer = byteBufAllocator.buffer(RESP_INTEGER_CAPACITY, RESP_INTEGER_CAPACITY);
    buffer.writeByte(INTEGER_ID);
    writeToString(value, buffer);
    buffer.writeBytes(CRLFar);
    return buffer;
  }

  // TODO optimize direct int -> ByteBuf as string
  static ByteBuf writeToString(final int value, final ByteBuf buffer) {
    if (value >= 0 && value <= 9) {
      buffer.writeByte('0' + value);
    } else {
      buffer.writeCharSequence(Integer.toString(value), StandardCharsets.UTF_8);
    }
    return buffer;
  }

  protected boolean onlySetOnAbsent() {
    return false;
  }
}
