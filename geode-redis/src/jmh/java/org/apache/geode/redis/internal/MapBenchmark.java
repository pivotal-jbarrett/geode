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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;


/**
 *
 * Benchmark                                         (impl)   Mode  Cnt         Score   Error  Units
 * MapBenchmark.containsKeyAndPut                   HashMap  thrpt       50336158.258          ops/s
 * MapBenchmark.containsKeyAndPut  Object2ObjectOpenHashMap  thrpt       45046494.882          ops/s
 * MapBenchmark.get                                 HashMap  thrpt       84261605.262          ops/s
 * MapBenchmark.get                Object2ObjectOpenHashMap  thrpt       59799047.618          ops/s
 * MapBenchmark.put                                 HashMap  thrpt       48290273.917          ops/s
 * MapBenchmark.put                Object2ObjectOpenHashMap  thrpt       51348095.124          ops/s
 * MapBenchmark.putIfAbsent                         HashMap  thrpt       66080613.503          ops/s
 * MapBenchmark.putIfAbsent        Object2ObjectOpenHashMap  thrpt       62525262.585          ops/s
 * MapBenchmark.replace                             HashMap  thrpt       63781845.155          ops/s
 * MapBenchmark.replace            Object2ObjectOpenHashMap  thrpt       44545188.132          ops/s
 */
@State(Scope.Benchmark)
@Fork(1)
public class MapBenchmark {
  private static final Integer[] cache = new Integer[1000];

  public Map<Integer, Integer> map;

  public enum Impl {
    HashMap, Object2ObjectOpenHashMap //, ConcurrentHashMap
  }

  @Param()
  public Impl impl;

  private final Random random = new Random();

  @Setup
  public void setup() {
    map = createMap(impl);
    for (int i = 0; i < cache.length; i++) {
      cache[i] = i;
      map.put(getKey(i), getValue(i));
    }
  }

  private static <K, V> Map<K, V> createMap(Impl impl) {
    switch (impl) {
      case HashMap:
        return new HashMap<>();
      case Object2ObjectOpenHashMap:
        return new Object2ObjectOpenHashMap<>();
//      case ConcurrentHashMap:
//        return new ConcurrentHashMap<>();
      default:
        throw new IllegalStateException();
    }
  }

  private static Integer getKey(final int i) {
    return cache[i];
  }

  private static Integer getValue(final int i) {
    return cache[i];
  }

  private int getIndex() {
    return random.nextInt(1000);
  }

  @Benchmark
  public Integer replace() {
    final int i = getIndex();
    return map.replace(getKey(i), getValue(i));
  }

  @Benchmark
  public Integer containsKeyAndPut() {
    final int i = getIndex();
    if (map.containsKey(getKey(i))) {
      return map.put(getKey(i), getValue(i));
    }
    return null;
  }

  @Benchmark
  public Integer put() {
    final int i = getIndex();
    return map.put(getKey(i), getValue(i));
  }

  @Benchmark
  public Integer get() {
    final int i = getIndex();
    return map.get(getKey(i));
  }

  @Benchmark
  public Integer putIfAbsent() {
    final int i = getIndex();
    return map.putIfAbsent(getKey(i), getValue(i));
  }


}
