#include "objclib.h"

#include <assert.h>
#include <dispatch/dispatch.h>
#include <pthread.h>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>

#import <stdatomic.h>

@implementation Event {
    volatile atomic_bool triggered_;
}
-(void)trigger {
    atomic_store(&triggered_, true);
}
-(BOOL)isTriggered {
    return atomic_load(&triggered_) ? YES : NO;
}
@end

@implementation OnDestroyHook {
    void (^onDestroy_)(uintptr_t);
}

- (uintptr_t)identity {
    return (uintptr_t)self;
}

- (instancetype)init:(void (^)(uintptr_t))onDestroy {
    if (self = [super init]) {
        [onDestroy retain];
        onDestroy_ = onDestroy;
    }
    return self;
}

- (void)dealloc {
    onDestroy_([self identity]);
    [super dealloc];
}

@end

void retain(uint64_t obj) {
    [((id) obj) retain];
}

void release(uint64_t obj) {
    [((id) obj) release];
}

void autorelease(uint64_t obj) {
    [((id) obj) autorelease];
}

@implementation Action {
    void (^action_)(uintptr_t);
}

- (uintptr_t)identity {
    return (uintptr_t)self;
}

- (instancetype)init:(void (^)(uintptr_t))action {
    if (self = [super init]) {
        [action retain];
        action_ = action;
    }
    return self;
}

- (void)scheduleWithTimer {
    [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(execute) userInfo:nil repeats:NO];
}

- (void)scheduleWithPerformSelector {
    [[NSRunLoop currentRunLoop] performSelector:@selector(execute) target:self argument:nil order:1 modes:@[NSDefaultRunLoopMode]];
}

- (void)execute {
    action_([self identity]);
}

@end

void startApp(void (^task)()) {
    dispatch_async(dispatch_get_main_queue(), ^{
        // At this point all other scheduled main queue tasks were already executed.
        // Executing via performBlock to allow a recursive run loop in `spin()`.
        [[NSRunLoop currentRunLoop] performBlock:^{
            task();
            [NSApp terminate:NULL];
        }];
    });
    [[NSApplication sharedApplication] run];
}

uint64_t currentThreadId() {
    uint64_t result;
    int ret = pthread_threadid_np(NULL, &result);
    assert(ret == 0);
    return result;
}

BOOL isMainThread() {
    return [NSThread isMainThread];
}

void spin() {
    if ([NSRunLoop currentRunLoop] != [NSRunLoop mainRunLoop]) {
        fprintf(stderr, "Must spin main run loop\n");
        exit(1);
    }
    [[NSRunLoop currentRunLoop]
           runMode:NSDefaultRunLoopMode
        beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
}
