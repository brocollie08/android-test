/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.test.platform.tracing;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.tracing.Trace;
import java.util.ArrayDeque;

/**
 * AndroidX Tracing wrapper implementation of the Tracer API.
 *
 * <p>Registering this tracer does not guarantee trace collection as the underlying Android tracing
 * feature has its own enabling logic. The Android tracing API depends on the runtime SDK API: <br>
 * - Below API 18 (JB MR2), AndroidX Tracing is a no-op as the underlying feature did not exist.<br>
 * - API 18 up to 28: Tracing is enabled by a call to {@link Trace#forceEnableAppTracing()}.<br>
 * - API 29 or 30: Tracing can also be enabled via a manifest flag. <br>
 * - API 31 and above: Tracing is enabled by default, unless it's disabled.
 *
 * <p>See details below in the AndroidX Tracing {@link Trace} documentation.
 *
 * <p>For API 18+, an instance of this wrapper is automatically created and registered in {@link
 * Tracing}.
 *
 * @see <a
 *     href="https://developer.android.com/reference/androidx/tracing/Trace">androidx.tracing.Trace</a>
 */
class AndroidXTracer implements Tracer {
  private static final String TAG = AndroidXTracer.class.getSimpleName();

  /** android.os.Trace.beginSection() has a limit on name length. */
  private static final int MAX_SECTION_NAME_LEN = 127;

  @NonNull
  @Override
  public Span beginSpan(@NonNull String name) {
    Trace.beginSection(sanitizeSpanName(name));
    return new AndroidXTracerSpan();
  }

  private static class AndroidXTracerSpan implements Span {
    private final ArrayDeque<AndroidXTracerSpan> nestedSpans = new ArrayDeque<>();

    @NonNull
    @Override
    public Span beginChildSpan(@NonNull String name) {
      Trace.beginSection(sanitizeSpanName(name));

      AndroidXTracerSpan span = new AndroidXTracerSpan();
      nestedSpans.add(span);
      return span;
    }

    @Override
    public void close() {
      AndroidXTracerSpan span;
      while ((span = nestedSpans.pollLast()) != null) {
        span.close();
      }

      Trace.endSection();
    }
  }

  /**
   * android.os.Trace.beginSection() has a hard limit on the name length and throws if the name is
   * too long. We shorten here with a warning if needed.
   */
  @NonNull
  private static String sanitizeSpanName(@NonNull String name) {
    if (name.length() > MAX_SECTION_NAME_LEN) {
      Log.w(TAG, "Span name exceeds limits: " + name);
      name = name.substring(0, MAX_SECTION_NAME_LEN);
    }
    return name;
  }
}
