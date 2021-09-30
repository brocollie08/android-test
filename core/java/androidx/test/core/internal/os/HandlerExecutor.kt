/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.test.core.internal.os

import android.os.Handler
import java.util.concurrent.Executor

/** A likely temporary utility class that redirects Executor calls to a Handler. */
class HandlerExecutor(val handler: Handler) : Executor {

  override fun execute(command: Runnable) {
    if (Thread.currentThread().equals(handler.looper.thread)) {
      command.run()
    } else {
      handler.post(command)
    }
  }
}