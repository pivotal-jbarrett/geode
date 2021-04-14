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

import static org.apache.geode.util.internal.UncheckedUtils.uncheckedCast;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import org.apache.geode.logging.internal.log4j.api.LogService;

public class DefaultNettyTransportFactory {
  private static final Logger log = LogService.getLogger();

  static final String KEY = DefaultNettyTransportFactory.class.getName();

  static final NettyTransportFactory instance = fromProperties(System.getProperties());

  static {
    log.info("Instance is {}", instance.getClass().getName());
  }

  static NettyTransportFactory getInstance() {
    return instance;
  }

  static NettyTransportFactory fromProperties(final Properties properties) {
    final String className =
        properties.getProperty(KEY, NioNettyTransportFactory.class.getName());
    try {
      final Class<? extends NettyTransportFactory> clazz = uncheckedCast(Class.forName(className));
      return clazz.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException(String.format(
          "System property %s with value %s is not a valid value.", KEY, className), e);
    }
  }
}
