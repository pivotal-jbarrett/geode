package org.apache.geode.redis.internal.executor.hash;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;

public class DummyCache {
  static final Map<ByteBuf, Map<ByteBuf, ByteBuf>> cache = new HashMap<>();
}
