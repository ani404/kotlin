/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "CallsChecker.hpp"
#include "GlobalData.hpp"
#include "Memory.h"
#include "Logging.hpp"
#include "Porting.h"

using namespace kotlin;

gcScheduler::GCScheduler::ThreadData::Impl::Impl(GCSchedulerData& scheduler, mm::ThreadData& thread) noexcept :
    scheduler_(static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(scheduler)),
    mutatorAssists_(scheduler_.mutatorAssists(), thread) {}

gcScheduler::GCScheduler::ThreadData::ThreadData(gcScheduler::GCScheduler& scheduler, mm::ThreadData& thread) noexcept :
    impl_(std_support::make_unique<Impl>(scheduler.gcData(), thread)) {}

gcScheduler::GCScheduler::ThreadData::~ThreadData() = default;

gcScheduler::GCScheduler::GCScheduler() noexcept :
    gcData_(std_support::make_unique<internal::GCSchedulerDataAdaptive<steady_clock>>(config_, []() noexcept {
        // This call acquires a lock, but the lock are always short-lived,
        // so we ignore thread state switching to avoid recursive safe points.
        CallsCheckerIgnoreGuard guard;
        return mm::GlobalData::Instance().gc().Schedule();
    })) {}

ALWAYS_INLINE void gcScheduler::GCScheduler::ThreadData::safePoint() noexcept {
    impl().mutatorAssists().safePoint();
}

void gcScheduler::GCScheduler::schedule() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(gcData()).schedule();
}

void gcScheduler::GCScheduler::scheduleAndWaitFinished() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    auto epoch = static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(gcData()).schedule();
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    mm::GlobalData::Instance().gc().WaitFinished(epoch);
}

void gcScheduler::GCScheduler::scheduleAndWaitFinalized() noexcept {
    RuntimeLogInfo({kTagGC}, "Scheduling GC manually");
    auto epoch = static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(gcData()).schedule();
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
}

ALWAYS_INLINE void gcScheduler::GCScheduler::onGCFinish(int64_t epoch, size_t aliveBytes) noexcept {
    static_cast<internal::GCSchedulerDataAdaptive<steady_clock>&>(gcData()).onGCFinish(epoch, aliveBytes);
}
