/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import objclib.*

import kotlin.native.concurrent.*
import kotlin.native.internal.test.testLauncherEntryPoint
import kotlin.system.exitProcess
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.cinterop.*

val timeout = 10.seconds

fun allocCollectable(ctor: () -> ULong): ULong = autoreleasepool {
    ctor()
}

fun waitTriggered(event: Event) {
    val timeoutMark = TimeSource.Monotonic.markNow() + timeout

    while (true) {
        kotlin.native.internal.GC.collect()
        if (event.isTriggered()) {
            return
        }
        assertFalse(timeoutMark.hasPassedNow(), "Timed out")
    }
}

@Test
fun testAutoreleaseOnSecondaryThread() {
    val event = withWorker {
        execute(TransferMode.SAFE, {}) {
            val event = Event()
            assertFalse(event.isTriggered())

            val victimId = allocCollectable {
                val v = OnDestroyHook {
                    event.triggerDirectly()
                }
                retain(v.identity())
                v.identity()
            }

            allocCollectable {
                OnDestroyHook {
                    autorelease(victimId)
                }.identity()
            }

            event
        }.result
    }
    waitTriggered(event)
}

@Test
fun testTimerOnSecondaryThread() {
    val event = Event()

    withWorker {
        execute(TransferMode.SAFE, { event }) { event ->
            allocCollectable {
                OnDestroyHook {
                    event.scheduleWithTimer()
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

@Test
fun testSelectorOnSecondaryThread() {
    val event = Event()

    withWorker {
        execute(TransferMode.SAFE, { event }) { event ->
            allocCollectable {
                OnDestroyHook {
                    event.scheduleWithPerformSelector()
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

@Test
fun testSelectorAfterDelayOnSecondaryThread() {
    val event = Event()

    withWorker {
        execute(TransferMode.SAFE, { event }) { event ->
            allocCollectable {
                OnDestroyHook {
                    event.scheduleWithPerformSelectorAfterDelay()
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

