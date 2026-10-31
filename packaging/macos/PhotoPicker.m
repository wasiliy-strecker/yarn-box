// Immediate file selection only. The image is copied into SQLite during this session.
// No persistent access to the source photograph or its directory is required.
#import <Cocoa/Cocoa.h>
#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>
#include <jni.h>

static NSString *fromJava(JNIEnv *env, jstring value) {
    if (value == NULL) return nil;
    const jchar *characters = (*env)->GetStringChars(env, value, NULL);
    if (characters == NULL) return nil;
    NSString *result = [[NSString alloc] initWithCharacters:(const unichar *)characters
                                                    length:(NSUInteger)(*env)->GetStringLength(env, value)];
    (*env)->ReleaseStringChars(env, value, characters);
    return result;
}

JNIEXPORT jstring JNICALL Java_de_appfabrik_garnbox_ui_MacPhotoPicker_choosePhoto(
    JNIEnv *env, jclass type, jstring title, jstring initialDirectory) {
    (void)type;
    @autoreleasepool {
        NSString *panelTitle = fromJava(env, title);
        if ((*env)->ExceptionCheck(env)) return NULL;
        NSString *directory = fromJava(env, initialDirectory);
        if ((*env)->ExceptionCheck(env)) return NULL;
        __block NSString *selected = nil;
        void (^choose)(void) = ^{
            NSOpenPanel *panel = [NSOpenPanel openPanel];
            panel.title = panelTitle ?: @"Yarn Box";
            panel.canChooseFiles = YES;
            panel.canChooseDirectories = NO;
            panel.allowsMultipleSelection = NO;
            panel.allowedContentTypes = @[UTTypeJPEG, UTTypePNG];
            if (directory.length > 0) {
                // Do not stat/canonicalize the parent before the Powerbox opens it.
                panel.directoryURL = [NSURL fileURLWithPath:directory isDirectory:YES];
            }
            if ([panel runModal] == NSModalResponseOK) selected = panel.URL.path;
        };
        if ([NSThread isMainThread]) choose();
        else dispatch_sync(dispatch_get_main_queue(), choose);
        if (selected == nil) return NULL;
        NSData *characters = [selected dataUsingEncoding:NSUTF16LittleEndianStringEncoding];
        return (*env)->NewString(env, (const jchar *)characters.bytes, (jsize)(characters.length / 2));
    }
}
