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
        epochDoneCallback_(std::move(epochDoneCallback)) {}

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
        finalizerQueueCondVar_.notify_all();
    }

    void StopFinalizerThread() noexcept {
        {
            std::unique_lock guard(finalizerQueueMutex_);
            if (!finalizerThread_.joinable()) return;
            shutdownFlag_ = true;
            finalizerQueueCondVar_.notify_all();
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
#if KONAN_OBJC_INTEROP
            // create a dummy input source to prevent the run loop from premature termination
            CFRunLoopSourceContext context{0, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, [](void* info) {}};
            auto dummyInputSource = CFRunLoopSourceCreate(nullptr, 0, &context);
            CFRunLoopAddSource(CFRunLoopGetCurrent(), dummyInputSource, kCFRunLoopDefaultMode);
#endif
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = true;
            }
            initializedCondVar_.notify_all();
            int64_t finalizersEpoch = 0;
            while (true) {
                std::unique_lock lock(finalizerQueueMutex_);
                finalizerQueueCondVar_.wait(lock, [this, &finalizersEpoch] {
                    return !FinalizerQueueTraits::isEmpty(finalizerQueue_) || finalizerQueueEpoch_ != finalizersEpoch || shutdownFlag_;
                });
                if (FinalizerQueueTraits::isEmpty(finalizerQueue_) && finalizerQueueEpoch_ == finalizersEpoch) {
                    newTasksAllowed_ = false;
                    RuntimeAssert(shutdownFlag_, "Nothing to do, but no shutdownFlag_ is set on wakeup");
                    break;
                }
                auto queue = std::move(finalizerQueue_);
                finalizersEpoch = finalizerQueueEpoch_;
                lock.unlock();
                if (!FinalizerQueueTraits::isEmpty(queue)) {
                    ThreadStateGuard guard(ThreadState::kRunnable);
#if KONAN_OBJC_INTEROP
                    konan::AutoreleasePool autoreleasePool;
#endif
                    FinalizerQueueTraits::process(std::move(queue));
                    processObjcRunLoop();
                }
                epochDoneCallback_(finalizersEpoch);
            }
            processObjcRunLoop();
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = false;
            }
            initializedCondVar_.notify_all();
        });
    }

    void processObjcRunLoop() {
#if KONAN_OBJC_INTEROP
        while (true) {
            konan::AutoreleasePool autoreleasePool;
            auto res = CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.1, TRUE);
            if (res != kCFRunLoopRunHandledSource) break;
        }
#endif
    }

    void WaitFinalizerThreadInitialized() noexcept {
        std::unique_lock guard(initializedMutex_);
        initializedCondVar_.wait(guard, [this] { return initialized_; });
    }

private:
    ScopedThread finalizerThread_;
    FinalizerQueue finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t finalizerQueueEpoch_ = 0;
    bool shutdownFlag_ = false;
    bool newTasksAllowed_ = true;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;
};

} // namespace kotlin::gc
