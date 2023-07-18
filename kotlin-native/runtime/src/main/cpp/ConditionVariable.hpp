/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cinttypes>
#include <thread>

#include "Utils.hpp"

namespace kotlin {

// `std::condition_variable_any` implemented via spinning on atomics.
// Additionally it uses `kotlin::steady_clock` for `wait_for`.
class ConditionVariableSpin : private Pinned {
public:
    void notify_one() noexcept {
        // Conditional variable does not protect the data: a mutex must be
        // used to protect it, so we don't need synchronization.
        epoch_.fetch_add(1, std::memory_order_relaxed);
    }

    void notify_all() noexcept {
        // Conditional variable does not protect the data: a mutex must be
        // used to protect it, so we don't need synchronization.
        epoch_.fetch_add(1, std::memory_order_relaxed);
    }

    template <typename Lock>
    void wait(Lock& lock) {
        auto currentEpoch = epoch_.load(std::memory_order_relaxed);
        lock.unlock();
        // Waiting for any change of the epoch.
        while (epoch_.load(std::memory_order_relaxed) == currentEpoch) {
            std::this_thread::yield();
        }
        lock.lock();
    }

    template <typename Lock, typename Predicate>
    void wait(Lock& lock, Predicate stopWaiting) {
        while (!stopWaiting()) {
            wait(lock);
        }
    }

    // TODO: Implement wait_until and wait_for using kotlin::steady_clock.

private:
    std::atomic<size_t> epoch_ = 0;
};

} // namespace kotlin
