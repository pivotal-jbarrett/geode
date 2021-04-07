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
 *
 */
package org.apache.geode.redis.internal.netty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Test;

import org.apache.geode.redis.internal.RedisCommandType;


/**
 * Test case for the command
 *
 *
 */
public class CommandJUnitTest {

  /**
   * Test method for {@link org.apache.geode.redis.internal.netty.Command#Command(java.util.List)}.
   */
  @Test
  public void testCommand() {
    final List<ByteBuf> list1 = null;
    assertThatThrownBy(() -> new Command(list1))
        .hasMessageContaining("List of command elements cannot be empty");

    final List<ByteBuf> list2 = new ArrayList<>();

    assertThatThrownBy(() -> new Command(list2))
        .hasMessageContaining("List of command elements cannot be empty");

    final List<ByteBuf> list3 = new ArrayList<>();
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
    buffer.writeCharSequence("Garbage", StandardCharsets.UTF_8);
    list3.add(buffer);

    Command cmd = new Command(list3);
    assertThat(cmd.getCommandType()).isNotNull();

    assertThat(cmd.getCommandType()).isEqualTo(RedisCommandType.UNKNOWN);
    list3.clear();
    buffer = ByteBufAllocator.DEFAULT.buffer();
    buffer.writeCharSequence(RedisCommandType.HEXISTS.toString(), StandardCharsets.UTF_8);
    list3.add(buffer);
    cmd = new Command(list3);
    assertThat(cmd.getCommandType()).isNotNull();
    assertThat(cmd.getCommandType()).isEqualTo(RedisCommandType.HEXISTS);
    assertThat(cmd.getProcessedCommand()).isEqualTo(list3);
    assertThat(cmd.getKey()).isNull();

    buffer = ByteBufAllocator.DEFAULT.buffer();
    buffer.writeCharSequence("Arg1", StandardCharsets.UTF_8);
    list3.add(buffer);
    cmd = new Command(list3);
    assertThat(cmd.getKey()).isNotNull();
    assertThat(cmd.getStringKey()).isEqualTo("Arg1");
  }
}
