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

package org.apache.geode.redis.internal;

import static io.netty.buffer.Unpooled.copiedBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;


/**
 * Benchmark (impl) Mode Cnt Score Error Units
 * ByteBufBenchmark.equalsWithPooledDirectByteBuf WrappedByteBuf thrpt 12961392.399 ops/s
 * ByteBufBenchmark.equalsWithPooledDirectByteBuf UnpooledDirectByteBuf thrpt 21228199.586 ops/s
 * ByteBufBenchmark.equalsWithPooledDirectByteBuf PooledDirectByteBuf thrpt 21641407.485 ops/s
 * ByteBufBenchmark.equalsWithPooledDirectByteBuf UnpooledHeapByteByf thrpt 22641954.299 ops/s
 * ByteBufBenchmark.equalsWithPooledDirectByteBuf PooledHeapByteBuf thrpt 17313972.171 ops/s
 * ByteBufBenchmark.equalsWithPooledHeapByteBuf WrappedByteBuf thrpt 12140482.481 ops/s
 * ByteBufBenchmark.equalsWithPooledHeapByteBuf UnpooledDirectByteBuf thrpt 20557705.808 ops/s
 * ByteBufBenchmark.equalsWithPooledHeapByteBuf PooledDirectByteBuf thrpt 19153928.742 ops/s
 * ByteBufBenchmark.equalsWithPooledHeapByteBuf UnpooledHeapByteByf thrpt 18627883.889 ops/s
 * ByteBufBenchmark.equalsWithPooledHeapByteBuf PooledHeapByteBuf thrpt 19417939.694 ops/s
 * ByteBufBenchmark.equalsWithUnpooledDirectByteBuf WrappedByteBuf thrpt 13851773.640 ops/s
 * ByteBufBenchmark.equalsWithUnpooledDirectByteBuf UnpooledDirectByteBuf thrpt 20627124.982 ops/s
 * ByteBufBenchmark.equalsWithUnpooledDirectByteBuf PooledDirectByteBuf thrpt 18939992.995 ops/s
 * ByteBufBenchmark.equalsWithUnpooledDirectByteBuf UnpooledHeapByteByf thrpt 22495306.612 ops/s
 * ByteBufBenchmark.equalsWithUnpooledDirectByteBuf PooledHeapByteBuf thrpt 18162390.185 ops/s
 * ByteBufBenchmark.equalsWithUnpooledHeapByteByf WrappedByteBuf thrpt 14060410.560 ops/s
 * ByteBufBenchmark.equalsWithUnpooledHeapByteByf UnpooledDirectByteBuf thrpt 20405674.801 ops/s
 * ByteBufBenchmark.equalsWithUnpooledHeapByteByf PooledDirectByteBuf thrpt 22916023.257 ops/s
 * ByteBufBenchmark.equalsWithUnpooledHeapByteByf UnpooledHeapByteByf thrpt 23242293.082 ops/s
 * ByteBufBenchmark.equalsWithUnpooledHeapByteByf PooledHeapByteBuf thrpt 20303997.744 ops/s
 * ByteBufBenchmark.equalsWithWrappedByteBuf WrappedByteBuf thrpt 11157415.696 ops/s
 * ByteBufBenchmark.equalsWithWrappedByteBuf UnpooledDirectByteBuf thrpt 13870803.149 ops/s
 * ByteBufBenchmark.equalsWithWrappedByteBuf PooledDirectByteBuf thrpt 12252969.929 ops/s
 * ByteBufBenchmark.equalsWithWrappedByteBuf UnpooledHeapByteByf thrpt 14889824.234 ops/s
 * ByteBufBenchmark.equalsWithWrappedByteBuf PooledHeapByteBuf thrpt 13234763.200 ops/s
 * ByteBufBenchmark.hashCode WrappedByteBuf thrpt 26781927.307 ops/s
 * ByteBufBenchmark.hashCode UnpooledDirectByteBuf thrpt 26910858.663 ops/s
 * ByteBufBenchmark.hashCode PooledDirectByteBuf thrpt 27803378.996 ops/s
 * ByteBufBenchmark.hashCode UnpooledHeapByteByf thrpt 25546758.117 ops/s
 * ByteBufBenchmark.hashCode PooledHeapByteBuf thrpt 26352572.423 ops/s
 *
 */
@State(Scope.Benchmark)
@Fork(1)
public class ByteBufBenchmark {
  static final byte[] byteArray = new byte[100];
  static {
    for (int i = 0; i < byteArray.length; i++) {
      byteArray[i] = (byte) i;
    }
  }

  static final ByteBuf wrappedByteBuf = filledByteBuf(Impl.WrappedByteBuf);
  static final ByteBuf unpooledDirectByteBuf = filledByteBuf(Impl.UnpooledDirectByteBuf);
  static final ByteBuf pooledDirectByteBuf = filledByteBuf(Impl.PooledDirectByteBuf);
  static final ByteBuf unpooledHeapByteByf = filledByteBuf(Impl.UnpooledHeapByteByf);
  static final ByteBuf pooledHeapByteBuf = filledByteBuf(Impl.PooledHeapByteBuf);

  public ByteBuf buffer;

  public enum Impl {
    WrappedByteBuf,
    UnpooledDirectByteBuf,
    PooledDirectByteBuf,
    UnpooledHeapByteByf,
    PooledHeapByteBuf
  }

  @Param()
  public Impl impl;

  @Setup
  public void setup() {
    buffer = filledByteBuf(impl);
  }

  private static ByteBuf filledByteBuf(final Impl impl) {
    final ByteBuf buffer = allocateByteBuf(impl, byteArray.length);
    buffer.writeBytes(byteArray);
    return buffer;
  }

  private static ByteBuf allocateByteBuf(final Impl impl, final int size) {
    switch (impl) {
      case WrappedByteBuf:
        return Unpooled.wrappedBuffer(new byte[size]).writerIndex(0);
      case UnpooledDirectByteBuf:
        copiedBuffer(byteArray);
        return UnpooledByteBufAllocator.DEFAULT.directBuffer(size);
      case PooledDirectByteBuf:
        return PooledByteBufAllocator.DEFAULT.directBuffer(size);
      case UnpooledHeapByteByf:
        return UnpooledByteBufAllocator.DEFAULT.heapBuffer(size);
      case PooledHeapByteBuf:
        return PooledByteBufAllocator.DEFAULT.heapBuffer(size);
      default:
        throw new IllegalStateException();
    }
  }

  @Benchmark
  public int hashCode() {
    return buffer.hashCode();
  }

  @Benchmark
  public boolean equalsWithWrappedByteBuf() {
    return buffer.equals(wrappedByteBuf);
  }

  @Benchmark
  public boolean equalsWithUnpooledDirectByteBuf() {
    return buffer.equals(unpooledDirectByteBuf);
  }

  @Benchmark
  public boolean equalsWithPooledDirectByteBuf() {
    return buffer.equals(pooledDirectByteBuf);
  }

  @Benchmark
  public boolean equalsWithUnpooledHeapByteByf() {
    return buffer.equals(unpooledHeapByteByf);
  }

  @Benchmark
  public boolean equalsWithPooledHeapByteBuf() {
    return buffer.equals(pooledHeapByteBuf);
  }

}
