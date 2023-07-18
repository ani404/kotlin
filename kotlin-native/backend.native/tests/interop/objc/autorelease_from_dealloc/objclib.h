#include <inttypes.h>
#include <objc/NSObject.h>

@interface OnDestroyHook: NSObject
-(instancetype)init:(void(^)(uintptr_t))onDestroy;
-(uintptr_t)identity;
@end

void retain(uint64_t);
void release(uint64_t);
void autorelease(uint64_t);

@interface Event: NSObject
-(void)scheduleWithTimer;
-(void)scheduleWithPerformSelector;
-(void)scheduleWithPerformSelectorAfterDelay;
-(void)triggerDirectly;
-(BOOL)isTriggered;
-(uintptr_t)identity;
@end
