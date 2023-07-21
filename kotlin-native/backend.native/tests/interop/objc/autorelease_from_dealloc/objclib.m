#include "objclib.h"

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
