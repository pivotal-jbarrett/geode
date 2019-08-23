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

import java.util.Collection;

/**
 * Provides a more natural {@link ResultSender} behavior when whether a result is last or not is
 * unknown. All {@link #sendResult(Object)} calls are buffered such that when {@link #close()} is
 * called
 * the last item in the buffer is sent via the underlying {@link ResultSender#lastResult(Object)}.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 *   try (BufferedResultSender<SomeType> resultSender = new BufferedResultSender<>(context.getResultSender()) {
 *     for (SomeType result : results) {
 *       resultSender.sendResult(result);
 *     }
 *   }
 * }
 * </pre>
 *
 * This snippet will send all the results in the results collection to the underlying
 * {@link ResultSender}.
 * </p>
 *
 * @param <T> type of result to send
 */
public class BufferedResultSender<T> implements ResultSender<T>, AutoCloseable {

  private enum State {
    EMPTY, BUFFERING, CLOSED
  }

  private final ResultSender<? super T> resultSender;
  private T buffer;
  private State state = State.EMPTY;

  public static <T> BufferedResultSender<T> buffered(ResultSender<? super T> resultSender) {
    return new BufferedResultSender<>(resultSender);
  }

  public BufferedResultSender(ResultSender<? super T> resultSender) {
    this.resultSender = resultSender;
  }

  /**
   * Sends buffered result by executing {@link ResultSender#lastResult(Object)} on underlying
   * {@link ResultSender}. Since this method comes from {@link AutoCloseable} you may use this
   * {@link ResultSender}
   * in a try with resource block.
   */
  @Override
  public void close() {
    if (state == State.CLOSED) {
      return;
    }

    sendLastAndClose();
  }

  /**
   * Flush any buffered result by executing {@link ResultSender#sendResult(Object)} on underlying
   * {@link ResultSender} and buffer new result.
   *
   * @param oneResult to buffer for sending.
   */
  @Override
  public void sendResult(T oneResult) {
    if (state == State.CLOSED) {
      return;
    }

    bufferResult(oneResult);
  }

  /**
   * Flushes any buffered result and executes {@link ResultSender#lastResult(Object)} on underlying
   * {@link ResultSender}. Provided for backwards compatibility with {@link ResultSender} API but
   * should not be used directly.
   *
   * @param lastResult to send
   * @deprecated Use {@link #sendResult(Object)} and {@link #close()}
   */
  @Override
  @Deprecated
  public void lastResult(T lastResult) {
    sendResult(lastResult);
    sendLastAndClose();
  }

  /**
   * Flushes any buffered result and executes {@link ResultSender#sendException(Throwable)} on
   * underlying
   * {@link ResultSender}.
   *
   * @param throwable to send
   */
  @Override
  public void sendException(final Throwable throwable) {
    if (state == State.CLOSED) {
      return;
    }

    flushBuffer();
    resultSender.sendException(throwable);
    buffer = null;
    state = State.CLOSED;
  }

  public void sendAllResults(final Collection<? extends T> results) {
    if (state == State.CLOSED) {
      return;
    }

    for (final T result : results) {
      bufferResult(result);
    }
  }

  private void bufferResult(final T oneResult) {
    flushBuffer();
    buffer = oneResult;
  }

  private void sendLastAndClose() {
    resultSender.lastResult(buffer);
    buffer = null;
    state = State.CLOSED;
  }

  private void flushBuffer() {
    if (state == State.EMPTY) {
      state = State.BUFFERING;
    } else {
      resultSender.sendResult(buffer);
    }
  }

}
