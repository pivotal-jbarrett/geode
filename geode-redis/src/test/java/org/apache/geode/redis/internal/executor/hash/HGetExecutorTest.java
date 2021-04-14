package org.apache.geode.redis.internal.executor.hash;

import static org.apache.geode.redis.internal.executor.hash.DummyCache.cache;
import static org.assertj.core.api.Assertions.assertThat;
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

public class HGetExecutorTest {

  @Test
  public void keyDoesNotExistMultipleGets() {
    cache.clear();

    final Command command = new Command(
        Stream.of("hget", "key1", "field1").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HGetExecutor hGetExecutor = new HGetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    final ByteBuf expected = wrappedBuffer("$-1\r\n");

    final ByteBuf actual = hGetExecutor.executeCommand2(command, executionHandlerContext);
    assertThat(actual).isEqualTo(expected);
    actual.release();

    assertThat(hGetExecutor.executeCommand2(command, executionHandlerContext)).isEqualTo(expected);
  }

  @Test
  public void fieldDoesNotExistMultipleGets() {
    cache.clear();
    final HashMap<ByteBuf, ByteBuf> hash = new HashMap<>();
    cache.put(wrappedBuffer("key1"), hash);

    final Command command = new Command(
        Stream.of("hget", "key1", "field1").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HGetExecutor hGetExecutor = new HGetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    final ByteBuf expected = wrappedBuffer("$-1\r\n");

    final ByteBuf actual = hGetExecutor.executeCommand2(command, executionHandlerContext);
    assertThat(actual).isEqualTo(expected);
    actual.release();
    assertThat(hGetExecutor.executeCommand2(command, executionHandlerContext)).isEqualTo(expected);
  }

  @Test
  public void fieldExistsMultipleGets() {
    cache.clear();
    final HashMap<ByteBuf, ByteBuf> hash = new HashMap<>();
    hash.put(wrappedBuffer("field1"), wrappedBuffer("value1"));
    cache.put(wrappedBuffer("key1"), hash);

    final Command command = new Command(
        Stream.of("hget", "key1", "field1").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HGetExecutor hGetExecutor = new HGetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    final ByteBuf expected = wrappedBuffer("$6\r\nvalue1\r\n").asReadOnly();

    final ByteBuf actual = hGetExecutor.executeCommand2(command, executionHandlerContext);
    assertThat(actual).isEqualTo(expected);
    actual.release();
    assertThat(hGetExecutor.executeCommand2(command, executionHandlerContext)).isEqualTo(expected);
  }

  private ByteBuf wrappedBuffer(final String value) {
    return Unpooled.wrappedBuffer(value.getBytes(StandardCharsets.UTF_8));
  }

}
