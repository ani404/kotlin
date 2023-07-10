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

val timeout = 1.minutes

fun allocCollectable(ctor: () -> ULong): ULong = autoreleasepool {
    ctor()
}

fun waitTriggered(event: Event) {
    assertTrue(isMainThread())

    val timeoutMark = TimeSource.Monotonic.markNow() + timeout

    //kotlin.native.internal.GC.collect()

    while (true) {
        kotlin.native.internal.GC.collect()
        spin()
        if (event.isTriggered()) {
            return
        }
        assertFalse(timeoutMark.hasPassedNow(), "Timed out")
    }
}

@Test
fun testAutoreleaseOnMainThread() {
    assertTrue(isMainThread())

    val event = Event()
    assertFalse(event.isTriggered())

    val victimId = allocCollectable {
        val v = OnDestroyHook {
            event.trigger()
        }
        retain(v.identity())
        v.identity()
    }

    allocCollectable {
        OnDestroyHook {
            autorelease(victimId)
        }.identity()
    }

    waitTriggered(event)
}

@Test
fun testAutoreleaseOnSecondaryThread() {
    val event = withWorker {
        execute(TransferMode.SAFE, {}) {
            assertFalse(isMainThread())

            val event = Event()
            assertFalse(event.isTriggered())

            val victimId = allocCollectable {
                val v = OnDestroyHook {
                    event.trigger()
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
fun testTimerOnMainThread() {
    assertTrue(isMainThread())

    val event = Event()
    val action = Action { event.trigger() }

    allocCollectable {
        OnDestroyHook {
            action.scheduleWithTimer()
        }.identity()
    }

    waitTriggered(event)
}

@Test
fun testTimerOnSecondaryThread() {
    val event = Event()
    val action = Action { event.trigger() }

    withWorker {
        execute(TransferMode.SAFE, { action }) { action ->
            assertFalse(isMainThread())

            allocCollectable {
                OnDestroyHook {
                    action.scheduleWithTimer()
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

@Test
fun testSelectorOnMainThread() {
    assertTrue(isMainThread())

    val event = Event()
    val action = Action { event.trigger() }

    allocCollectable {
        OnDestroyHook {
            action.scheduleWithPerformSelector()
        }.identity()
    }

    waitTriggered(event)
}

@Ignore // does not work for some reason
@Test
fun testSelectorOnSecondaryThread() {
    val event = Event()
    val action = Action { event.trigger() }

    withWorker {
        execute(TransferMode.SAFE, { action }) { action ->
            assertFalse(isMainThread())

            allocCollectable {
                OnDestroyHook {
                    action.scheduleWithPerformSelector()
                }.identity()
            }
        }
    }

    waitTriggered(event)
}

// TODO get rid of this main thread emulation stuff?
fun runAllTests(args: Array<String>) = startApp {
    val exitCode = testLauncherEntryPoint(args)
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}
