/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <functional>
#include <utility>

#include "GCSchedulerConfig.hpp"
#include "KAssert.h"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

namespace kotlin::mm {
class ThreadData;
}

namespace kotlin::gcScheduler {

class GCSchedulerData {
public:
    virtual ~GCSchedulerData() = default;

    // The protocol is: after the scheduler schedules the GC, the GC eventually calls `OnPerformFullGC`
    // when the collection has started, followed by `UpdateAliveSetBytes` when the marking has finished.
    // TODO: Consider returning a sort of future from the scheduleGC, and listen to it instead.

    // Always called by the GC thread.
    virtual void OnPerformFullGC() noexcept = 0;

    // Called by different mutator threads.
    virtual void SetAllocatedBytes(size_t bytes) noexcept = 0;
};

class GCScheduler : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        class Impl;

        ThreadData(GCScheduler&, mm::ThreadData&) noexcept;
        ~ThreadData();

        Impl& impl() noexcept { return *impl_; }

        void safePoint() noexcept;

    private:
        std_support::unique_ptr<Impl> impl_;
    };

    GCScheduler() noexcept;

    GCSchedulerConfig& config() noexcept { return config_; }
    GCSchedulerData& gcData() noexcept { return *gcData_; }

    // Can be called by any thread.
    void schedule() noexcept;

    // Can be called by any thread.
    void scheduleAndWaitFinished() noexcept;

    // Can be called by any thread.
    void scheduleAndWaitFinalized() noexcept;

    // Called by the GC thread only.
    void onGCFinish(int64_t epoch, size_t aliveBytes) noexcept;

private:
    GCSchedulerConfig config_;
    std_support::unique_ptr<GCSchedulerData> gcData_;
};

} // namespace kotlin::gcScheduler
