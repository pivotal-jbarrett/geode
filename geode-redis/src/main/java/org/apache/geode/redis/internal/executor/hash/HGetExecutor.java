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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import org.apache.geode.redis.internal.executor.RedisResponse;
import org.apache.geode.redis.internal.netty.Coder;
import org.apache.geode.redis.internal.netty.Command;
import org.apache.geode.redis.internal.netty.ExecutionHandlerContext;

/**
 * <pre>
 *
 * Implements the Redis HGET command to returns the value associated with field in the hash stored
 * at key.
 *
 * Examples:
 *
 * redis> HSET myhash field1 "foo" (integer) 1 redis> HGET myhash field1 "foo" redis> HGET myhash
 * field2
 *
 * <pre>
 */
public class HGetExecutor extends HashExecutor {


  @Override
  public RedisResponse executeCommand(Command command,
                                      ExecutionHandlerContext context) {
    throw new IllegalStateException();
  }

  @Override
  public ByteBuf executeCommand2(final Command command, final ExecutionHandlerContext context) {
    final List<ByteBuf> commandElems = command.getCommandParts();

    final ByteBuf key = commandElems.get(1);
    final ByteBuf field = commandElems.get(2);

    final Map<ByteBuf, ByteBuf> hash = cache.get(key);

    final ByteBuf value;
    if (null == hash) {
      value = null;
    } else {
      value = hash.get(field);
    }

    if (null == value) {
      return NIL.retain();
    }

    return toBulkString(value, context.getByteBufAllocator());
  }

  static final ByteBuf CRLF = Unpooled.directBuffer(2,2).writeByte('\r').writeByte('\n').asReadOnly();
  static final ByteBuf NIL = Unpooled.directBuffer(5, 5 ).writeByte('$').writeByte('-').writeByte('1').writeBytes(CRLF.duplicate()).asReadOnly();

  static final int RESP_BULK_STRING_COMPONENTS = 4; /*header, crlf, value, crlf*/
  static final int RESP_BULK_STRING_HEADER_CAPACITY = 1 /*$*/ + 11 /*int*/;

  static ByteBuf toBulkString(final ByteBuf value, final ByteBufAllocator byteBufAllocator) {
    final CompositeByteBuf buffer = byteBufAllocator.compositeBuffer(RESP_BULK_STRING_COMPONENTS);
    buffer.addComponent(true, HSetExecutor.writeToString(value.readableBytes(), byteBufAllocator.directBuffer(RESP_BULK_STRING_HEADER_CAPACITY,RESP_BULK_STRING_HEADER_CAPACITY).writeByte(Coder.BULK_STRING_ID)));
    buffer.addComponent(true, CRLF.retain());
    buffer.addComponent(true, value.retain());
    buffer.addComponent(true, CRLF.retain());
    return buffer;
  }
}
