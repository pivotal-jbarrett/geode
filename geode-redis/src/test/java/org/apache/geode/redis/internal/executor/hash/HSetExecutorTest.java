package org.apache.geode.redis.internal.executor.hash;

import static org.apache.geode.redis.internal.executor.hash.DummyCache.cache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import org.apache.geode.redis.internal.netty.Command;
import org.apache.geode.redis.internal.netty.ExecutionHandlerContext;

public class HSetExecutorTest {

  @Test
  public void mapContainsNewHash() {
    cache.clear();

    final Command command = new Command(
        Stream.of("hset", "key1", "field1", "value1").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HSetExecutor hSetExecutor = new HSetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    hSetExecutor.executeCommand2(command, executionHandlerContext);

    assertThat(cache.get(wrappedBuffer("key1")))
        .contains(entry(wrappedBuffer("field1"), wrappedBuffer("value1")));
  }

  @Test
  public void mapUpdatesExistingHash() {
    cache.clear();
    final HashMap<ByteBuf, ByteBuf> hash = new HashMap<>();
    hash.put(wrappedBuffer("field1"), wrappedBuffer("value1"));
    cache.put(wrappedBuffer("key1"), hash);

    final Command command = new Command(
        Stream.of("hset", "key1", "field1", "value2").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HSetExecutor hSetExecutor = new HSetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    hSetExecutor.executeCommand2(command, executionHandlerContext);

    assertThat(cache.get(wrappedBuffer("key1")))
        .contains(entry(wrappedBuffer("field1"), wrappedBuffer("value2")));
  }

  private ByteBuf wrappedBuffer(final String value) {
    return Unpooled.wrappedBuffer(value.getBytes(StandardCharsets.UTF_8));
  }

}
