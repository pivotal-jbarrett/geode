package org.apache.geode.redis.internal.executor.hash;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ByteBufTest {

  @Test
  public void readingReadOnlyByteBufAdvancesReadIndex() {
    final ByteBuf originalBuffer = Unpooled.buffer();
    originalBuffer.writeCharSequence("testing123", StandardCharsets.UTF_8);

    final ByteBuf readOnlyBuffer = originalBuffer.asReadOnly();

    assertThat(originalBuffer).isEqualTo(readOnlyBuffer);
    assertThat(originalBuffer.readableBytes()).isEqualTo(readOnlyBuffer.readableBytes());

    while (readOnlyBuffer.isReadable()) {
      readOnlyBuffer.readByte();
    }

    assertThat(originalBuffer).isNotEqualTo(readOnlyBuffer);
    assertThat(originalBuffer.readableBytes()).isNotEqualTo(readOnlyBuffer.readableBytes());
  }

  @Test
  public void readingCompositeByteBufDoesNotAdvanceComponentIndexes() {
    final CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
    final ByteBuf part1 = Unpooled.buffer().writeByte('a').writeByte('b').writeByte('c');
    final int part1ReadableBytes = part1.readableBytes();
    compositeByteBuf.addComponent(true, part1);
    final ByteBuf part2 = Unpooled.buffer().writeByte('d').writeByte('e').writeByte('f');
    final int part2ReadableBytes = part2.readableBytes();
    compositeByteBuf.addComponent(true, part2);

    final ByteBuf expectedBuffer = Unpooled.buffer();
    expectedBuffer.writeCharSequence("abcdef", StandardCharsets.UTF_8);

    assertThat(compositeByteBuf.writerIndex()).isEqualTo(expectedBuffer.writerIndex());
    assertThat(compositeByteBuf.readerIndex()).isEqualTo(expectedBuffer.readerIndex());
    assertThat(compositeByteBuf.readableBytes()).isEqualTo(expectedBuffer.readableBytes());
    assertThat(expectedBuffer).isEqualTo(compositeByteBuf);

    while (compositeByteBuf.isReadable()) {
      assertThat((char) compositeByteBuf.readByte()).isEqualTo((char) expectedBuffer.readByte());
    }

    assertThat(compositeByteBuf.writerIndex()).isEqualTo(expectedBuffer.writerIndex());
    assertThat(compositeByteBuf.readerIndex()).isEqualTo(expectedBuffer.readerIndex());
    assertThat(compositeByteBuf.readableBytes()).isEqualTo(expectedBuffer.readableBytes());
    assertThat(expectedBuffer).isEqualTo(compositeByteBuf);

    assertThat(part1.readableBytes()).isEqualTo(part1ReadableBytes);
    assertThat(part2.readableBytes()).isEqualTo(part2ReadableBytes);
  }
}
