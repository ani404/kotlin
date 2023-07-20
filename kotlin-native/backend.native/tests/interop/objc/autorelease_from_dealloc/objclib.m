#include "objclib.h"

#include <assert.h>
#include <dispatch/dispatch.h>
#include <pthread.h>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>
#import <stdatomic.h>

@implementation OnDestroyHook {
    void (^onDestroy_)(uintptr_t);
}

-(uintptr_t)identity {
    return (uintptr_t)self;
}

-(instancetype)init:(void (^)(uintptr_t))onDestroy {
    if (self = [super init]) {
        [onDestroy retain];
        onDestroy_ = onDestroy;
    }
    return self;
}

-(void)dealloc {
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

@implementation Event {
    volatile atomic_bool triggered_;
}

-(uintptr_t)identity {
    return (uintptr_t)self;
}

-(void)scheduleWithTimer {
    assert(![self isTriggered]);
    [NSTimer scheduledTimerWithTimeInterval:0 target:self selector:@selector(triggerDirectly) userInfo:nil repeats:NO];
}

-(void)scheduleWithPerformSelector {
    assert(![self isTriggered]);
    [[NSRunLoop currentRunLoop] performSelector:@selector(triggerDirectly) target:self argument:nil order:0 modes:@[NSDefaultRunLoopMode]];
}

-(void)scheduleWithPerformSelectorAfterDelay {
    assert(![self isTriggered]);
    [self performSelector:@selector(triggerDirectly) withObject:self afterDelay:0];
}

-(void)scheduleWithPerformBlock {
    assert(![self isTriggered]);
    [[NSRunLoop currentRunLoop] performBlock:^{
        [self triggerDirectly];
    }];
}

-(void)triggerDirectly {
    assert(![self isTriggered]);
    atomic_store(&triggered_, true);
}

-(BOOL)isTriggered {
    return atomic_load(&triggered_) ? YES : NO;
}

@end
