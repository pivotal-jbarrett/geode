package org.apache.geode.redis.internal;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ByteBufBenchmarkTest {

  @Test
  public void readableBytesEqualsByteArrayLength() {
    ByteBufBenchmark byteBufBenchmark = new ByteBufBenchmark();
    byteBufBenchmark.impl = ByteBufBenchmark.Impl.PooledDirectByteBuf;
    byteBufBenchmark.setup();

    assertThat(byteBufBenchmark.buffer.readableBytes())
        .isEqualTo(ByteBufBenchmark.byteArray.length);
    assertThat(ByteBufBenchmark.wrappedByteBuf.readableBytes())
        .isEqualTo(ByteBufBenchmark.byteArray.length);
    assertThat(ByteBufBenchmark.unpooledDirectByteBuf.readableBytes())
        .isEqualTo(ByteBufBenchmark.byteArray.length);
    assertThat(ByteBufBenchmark.pooledDirectByteBuf.readableBytes())
        .isEqualTo(ByteBufBenchmark.byteArray.length);
    assertThat(ByteBufBenchmark.unpooledHeapByteByf.readableBytes())
        .isEqualTo(ByteBufBenchmark.byteArray.length);
    assertThat(ByteBufBenchmark.pooledHeapByteBuf.readableBytes())
        .isEqualTo(ByteBufBenchmark.byteArray.length);
  }

  @Test
  public void testEquals() {
    ByteBufBenchmark byteBufBenchmark = new ByteBufBenchmark();
    byteBufBenchmark.impl = ByteBufBenchmark.Impl.PooledDirectByteBuf;
    byteBufBenchmark.setup();

    assertThat(byteBufBenchmark.buffer).isEqualTo(ByteBufBenchmark.wrappedByteBuf);
    assertThat(byteBufBenchmark.buffer).isEqualTo(ByteBufBenchmark.unpooledDirectByteBuf);
    assertThat(byteBufBenchmark.buffer).isEqualTo(ByteBufBenchmark.pooledDirectByteBuf);
    assertThat(byteBufBenchmark.buffer).isEqualTo(ByteBufBenchmark.unpooledHeapByteByf);
    assertThat(byteBufBenchmark.buffer).isEqualTo(ByteBufBenchmark.pooledHeapByteBuf);
  }

  @Test
  public void testHashCode() {
    ByteBufBenchmark byteBufBenchmark = new ByteBufBenchmark();
    byteBufBenchmark.impl = ByteBufBenchmark.Impl.PooledDirectByteBuf;
    byteBufBenchmark.setup();

    assertThat(byteBufBenchmark.buffer.hashCode())
        .isEqualTo(ByteBufBenchmark.wrappedByteBuf.hashCode());
    assertThat(byteBufBenchmark.buffer.hashCode())
        .isEqualTo(ByteBufBenchmark.unpooledDirectByteBuf.hashCode());
    assertThat(byteBufBenchmark.buffer.hashCode())
        .isEqualTo(ByteBufBenchmark.pooledDirectByteBuf.hashCode());
    assertThat(byteBufBenchmark.buffer.hashCode())
        .isEqualTo(ByteBufBenchmark.unpooledHeapByteByf.hashCode());
    assertThat(byteBufBenchmark.buffer.hashCode())
        .isEqualTo(ByteBufBenchmark.pooledHeapByteBuf.hashCode());
  }

}
