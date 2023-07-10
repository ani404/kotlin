#include <inttypes.h>
#include <objc/NSObject.h>

@interface Event: NSObject
-(void)trigger;
-(BOOL)isTriggered;
@end

@interface OnDestroyHook: NSObject
-(instancetype)init:(void(^)(uintptr_t))onDestroy;
-(uintptr_t)identity;
@end

void retain(uint64_t);
void release(uint64_t);
void autorelease(uint64_t);

@interface Action: NSObject
-(instancetype)init:(void(^)(uintptr_t))onDestroy;
-(void) scheduleWithTimer;
-(void)scheduleWithPerformSelector;
-(uintptr_t)identity;
@end

void startApp(void(^task)());
uint64_t currentThreadId();
BOOL isMainThread();
void spin();
