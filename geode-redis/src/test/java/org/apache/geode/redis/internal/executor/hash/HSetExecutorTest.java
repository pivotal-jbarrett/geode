package org.apache.geode.redis.internal.executor.hash;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.junit.Test;

import org.apache.geode.redis.internal.netty.Command;
import org.apache.geode.redis.internal.netty.ExecutionHandlerContext;

public class HSetExecutorTest {

  @Test
  public void mapContainsNewHash() {
    HSetExecutor.data.clear();

    final Command command = new Command(
        Stream.of("hset", "key1", "field1", "value1").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HSetExecutor hSetExecutor = new HSetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    hSetExecutor.executeCommand2(command, executionHandlerContext);

    assertThat(HSetExecutor.data).containsKey(wrappedBuffer("key1".getBytes()));
  }

  @Test
  public void mapUpdatesExistingHash() {
    HSetExecutor.data.clear();
    final Object2ObjectOpenHashMap<ByteBuf, byte[]> hash = new Object2ObjectOpenHashMap<>();
    hash.put(wrappedBuffer("field1".getBytes()), "value1".getBytes());
    HSetExecutor.data.put(wrappedBuffer("key1".getBytes()), hash);

    final Command command = new Command(
        Stream.of("hset", "key1", "field1", "value2").map(String::getBytes).map(
            Unpooled::wrappedBuffer).collect(Collectors.toList()));
    final HSetExecutor hSetExecutor = new HSetExecutor();

    final ExecutionHandlerContext executionHandlerContext = mock(ExecutionHandlerContext.class);
    when(executionHandlerContext.getByteBufAllocator()).thenReturn(ByteBufAllocator.DEFAULT);

    hSetExecutor.executeCommand2(command, executionHandlerContext);

    assertThat(HSetExecutor.data.get(wrappedBuffer("key1".getBytes())))
        .contains(entry(wrappedBuffer("field1".getBytes()), "value2".getBytes()));
  }

}