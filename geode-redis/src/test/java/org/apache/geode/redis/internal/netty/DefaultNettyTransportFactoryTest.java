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

import static org.apache.geode.redis.internal.netty.DefaultNettyTransportFactory.KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;

import org.junit.Test;

public class DefaultNettyTransportFactoryTest {

  @Test
  public void throwsIfInvalidProperty() {
    final Properties properties = new Properties();
    properties.setProperty(KEY, "some.class.that.does.not.exist");
    assertThatThrownBy(() -> DefaultNettyTransportFactory.fromProperties(properties))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void defaultIsNioNettyTransportFactory() {
    final Properties properties = new Properties();
    final NettyTransportFactory factory = DefaultNettyTransportFactory.fromProperties(properties);
    assertThat(factory).isInstanceOf(NioNettyTransportFactory.class);
  }

}
