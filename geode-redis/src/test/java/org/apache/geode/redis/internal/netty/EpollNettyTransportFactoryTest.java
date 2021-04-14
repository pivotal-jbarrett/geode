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
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import org.junit.Test;

public class EpollNettyTransportFactoryTest {

  @Test
  public void throwsWhenNotAvailable() {
    assumeThat(Epoll.isAvailable()).isFalse();

    assertThatThrownBy(EpollNettyTransportFactory::new).isInstanceOf(UnsatisfiedLinkError.class);
  }

  @Test
  public void createEventLoopGroup() {
    assumeThat(Epoll.isAvailable()).isTrue();

    final EpollNettyTransportFactory factory = new EpollNettyTransportFactory();
    final ThreadFactory mock = mock(ThreadFactory.class);
    final EventLoopGroup eventLoopGroup = factory.createEventLoopGroup(0, mock);
    assertThat(eventLoopGroup).isInstanceOf(EpollEventLoopGroup.class);
  }

  @Test
  public void getServerChannelClass() {
    assumeThat(Epoll.isAvailable()).isTrue();

    final EpollNettyTransportFactory factory = new EpollNettyTransportFactory();
    final Class<? extends ServerChannel> serverChannelClass = factory.getServerChannelClass();
    assertThat(serverChannelClass).isEqualTo(EpollServerSocketChannel.class);
  }
}
