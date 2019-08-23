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

package org.apache.geode.cache.execute;

import static java.util.Arrays.asList;
import static org.apache.geode.cache.execute.BufferedResultSender.buffered;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Test;

public class BufferedResultSenderTest {

  @SuppressWarnings("unchecked")
  private final ResultSender<Number> mockResultSender = mock(ResultSender.class);

  @Test
  public void closeInvokesLastResult() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.close();

    verify(mockResultSender).lastResult(null);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void closeAfterCloseIsNoop() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.close();
    resultSender.close();

    verify(mockResultSender).lastResult(null);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void autoCloseInvokesLastResult() {
    try (final BufferedResultSender<Integer> resultSender = buffered(mockResultSender)) {
      // need to do something here to keep static analyzer happy
      assertThat(resultSender).isNotNull();
    }

    verify(mockResultSender).lastResult(null);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void closeFlushesBuffer() {
    try (final BufferedResultSender<Integer> resultSender = buffered(mockResultSender)) {
      resultSender.sendResult(1);
      resultSender.sendResult(2);
    }

    verify(mockResultSender).sendResult(1);
    verify(mockResultSender).lastResult(2);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void sendResultBuffersFirstResult() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.sendResult(1);

    verifyZeroInteractions(mockResultSender);
  }

  @Test
  public void sendResultSendsFirstBuffersSecondResult() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.sendResult(1);
    resultSender.sendResult(2);

    verify(mockResultSender).sendResult(1);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void sendResultAfterCloseIsNoop() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.close();
    resultSender.sendResult(1);

    verify(mockResultSender).lastResult(null);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void lastResultFlushesAndCloses() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.lastResult(1);

    verify(mockResultSender).lastResult(1);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void sendExceptionWithoutResultsSendsException() {
    final Exception throwable = new Exception();

    try (final BufferedResultSender<Integer> resultSender = buffered(mockResultSender)) {
      resultSender.sendException(throwable);
    }

    verify(mockResultSender).sendException(throwable);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void sendExceptionWithoutResultFlushesAndSendsException() {
    final Exception throwable = new Exception();

    try (final BufferedResultSender<Integer> resultSender = buffered(mockResultSender)) {
      resultSender.sendResult(1);
      resultSender.sendException(throwable);
    }

    verify(mockResultSender).sendResult(1);
    verify(mockResultSender).sendException(throwable);
    verifyNoMoreInteractions(mockResultSender);
  }

  @Test
  public void sendAllResults() {
    try (final BufferedResultSender<Integer> resultSender = buffered(mockResultSender)) {
      resultSender.sendAllResults(asList(1, 2, 3));
    }

    verify(mockResultSender).sendResult(1);
    verify(mockResultSender).sendResult(2);
    verify(mockResultSender).lastResult(3);
    verifyNoMoreInteractions(mockResultSender);
  }


  @Test
  public void sendAllResultsAfterCloseIsNoop() {
    final BufferedResultSender<Integer> resultSender = new BufferedResultSender<>(mockResultSender);

    resultSender.close();
    resultSender.sendAllResults(asList(1, 2, 3));

    verify(mockResultSender).lastResult(null);
    verifyNoMoreInteractions(mockResultSender);
  }
}
