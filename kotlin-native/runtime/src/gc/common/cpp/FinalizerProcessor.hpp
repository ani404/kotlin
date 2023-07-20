/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <cstdint>
#include <functional>
#include <mutex>
#include <utility>

#include "KAssert.h"
#include "Memory.h"
#include "ObjectFactory.hpp"
#include "Runtime.h"
#include "ScopedThread.hpp"
#include "Utils.hpp"
#include "Logging.hpp"

#if KONAN_OBJC_INTEROP
#include "ObjCMMAPI.h"
#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFRunLoop.h>
#endif

namespace kotlin::gc {

template <typename FinalizerQueue, typename FinalizerQueueTraits>
class FinalizerProcessor : private Pinned {
public:
    // epochDoneCallback could be called on any subset of them.
    // If no new tasks are set, epochDoneCallback will be eventually called on last epoch
    explicit FinalizerProcessor(std::function<void(int64_t)> epochDoneCallback) noexcept :
        epochDoneCallback_(std::move(epochDoneCallback)), processingLoop_(*this) {}

    ~FinalizerProcessor() { StopFinalizerThread(); }

    void ScheduleTasks(FinalizerQueue tasks, int64_t epoch) noexcept {
        std::unique_lock guard(finalizerQueueMutex_);
        if (FinalizerQueueTraits::isEmpty(tasks) && !IsRunning()) {
            epochDoneCallback_(epoch);
            return;
        }
        finalizerQueueCondVar_.wait(guard, [this] { return newTasksAllowed_; });
        StartFinalizerThreadIfNone();
        FinalizerQueueTraits::add(finalizerQueue_, std::move(tasks));
        finalizerQueueEpoch_ = epoch;
        processingLoop_.notify();
    }

    void StopFinalizerThread() noexcept {
        {
            std::unique_lock guard(finalizerQueueMutex_);
            if (!finalizerThread_.joinable()) return;
            shutdownFlag_ = true;
            processingLoop_.notify();
        }
        finalizerThread_.join();
        shutdownFlag_ = false;
        RuntimeAssert(FinalizerQueueTraits::isEmpty(finalizerQueue_), "Finalizer queue should be empty when killing finalizer thread");
        std::unique_lock guard(finalizerQueueMutex_);
        newTasksAllowed_ = true;
        finalizerQueueCondVar_.notify_all();
    }

    bool IsRunning() const noexcept { return finalizerThread_.joinable(); }

    void StartFinalizerThreadIfNone() noexcept {
        std::unique_lock guard(threadCreatingMutex_);
        if (finalizerThread_.joinable()) return;

        finalizerThread_ = ScopedThread(ScopedThread::attributes().name("GC finalizer processor"), [this] {
            Kotlin_initRuntimeIfNeeded();
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = true;
            }
            initializedCondVar_.notify_all();
            processingLoop_.body();
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = false;
            }
            initializedCondVar_.notify_all();
        });
    }

    void WaitFinalizerThreadInitialized() noexcept {
        std::unique_lock guard(initializedMutex_);
        initializedCondVar_.wait(guard, [this] { return initialized_; });
    }

private:
    int64_t processSingle(int64_t previousEpoch) {
        std::unique_lock lock(finalizerQueueMutex_);
        finalizerQueueCondVar_.wait(lock, [this, &previousEpoch] {
            return !FinalizerQueueTraits::isEmpty(finalizerQueue_) || finalizerQueueEpoch_ != previousEpoch || shutdownFlag_;
        });
        if (FinalizerQueueTraits::isEmpty(finalizerQueue_) && finalizerQueueEpoch_ == previousEpoch) {
            newTasksAllowed_ = false;
            RuntimeAssert(shutdownFlag_, "Nothing to do, but no shutdownFlag_ is set on wakeup");
            return 0;
        }
        auto queue = std::move(finalizerQueue_);
        int64_t currentEpoch = finalizerQueueEpoch_;
        lock.unlock();
        if (!FinalizerQueueTraits::isEmpty(queue)) {
#if KONAN_OBJC_INTEROP
            konan::AutoreleasePool autoreleasePool;
#endif
            ThreadStateGuard guard(ThreadState::kRunnable);
            FinalizerQueueTraits::process(std::move(queue));
        }
        epochDoneCallback_(currentEpoch);
        return currentEpoch;
    }

#if KONAN_OBJC_INTEROP
    class ProcessingLoop {
    public:
        explicit ProcessingLoop(FinalizerProcessor& owner) :
                owner_(owner),
                sourceContext_{
                        0, this, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr,
                        [](void* info) {
                            auto& self = *reinterpret_cast<ProcessingLoop*>(info);
                            self.finishedEpoch_ = self.owner_.processSingle(self.finishedEpoch_);
                            if (self.finishedEpoch_ == 0) {
                                CFRunLoopStop(self.runLoop_.load(std::memory_order_relaxed));
                            }
                        }},
                runLoopSource_(CFRunLoopSourceCreate(nullptr, 0, &sourceContext_)) {}

        ~ProcessingLoop() {
            CFRelease(runLoopSource_);
        }

        void notify() {
            // wait until runLoop_ ptr is published
            while (runLoop_.load(std::memory_order_acquire) == nullptr) {
                std::this_thread::yield();
            }
            // notify
            CFRunLoopSourceSignal(runLoopSource_);
            CFRunLoopWakeUp(runLoop_);
        }

        void body() {
            konan::AutoreleasePool autoreleasePool;
            auto mode = kCFRunLoopDefaultMode;
            CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource_, mode);
            runLoop_.store(CFRunLoopGetCurrent(), std::memory_order_release);

            CFRunLoopRun();

            runLoop_.store(nullptr, std::memory_order_release);

            CFRunLoopRemoveSource(CFRunLoopGetCurrent(), runLoopSource_, mode);
        }
    private:
        FinalizerProcessor& owner_;
        int64_t finishedEpoch_ = 0;
        CFRunLoopSourceContext sourceContext_;
        std::atomic<CFRunLoopRef> runLoop_ = nullptr;
        CFRunLoopSourceRef runLoopSource_;
    };
#else
    class ProcessingLoop {
    public:
        explicit ProcessingLoop(FinalizerProcessor& owner) : owner_(owner) {}

        void notify() {
            owner_.finalizerQueueCondVar_.notify_all();
        }

        void body() {
            while (true) {
                finishedEpoch_ = owner_.processSingle(finishedEpoch_);
                if (finishedEpoch_ == 0) break;
            }
        }
    private:
        FinalizerProcessor& owner_;
        int64_t finishedEpoch_ = 0;
    };
#endif

    ScopedThread finalizerThread_;
    FinalizerQueue finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t finalizerQueueEpoch_ = 0;
    bool shutdownFlag_ = false;
    bool newTasksAllowed_ = true;

    ProcessingLoop processingLoop_;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;

};

} // namespace kotlin::gc
