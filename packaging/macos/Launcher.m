// App-local launcher. The pinned JDK's JLI entry point starts Java in this process.
// Native compilation and signed sandbox acceptance must be performed on macOS.
#import <Cocoa/Cocoa.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef int (*LaunchJava)(int, char **, int, const char **, int, const char **,
                         const char *, const char *, const char *, const char *,
                         unsigned char, unsigned char, unsigned char, int);

static int fail(NSString *message) {
    fprintf(stderr, "%s\n", message.UTF8String);
    NSAlert *alert = [[NSAlert alloc] init];
    alert.messageText = @"Yarn Box";
    alert.informativeText = message;
    [alert runModal];
    return 1;
}

int main(int argc, char **argv) {
    @autoreleasepool {
#if defined(__arm64__)
        NSString *arch = @"arm64";
#elif defined(__x86_64__)
        NSString *arch = @"x86_64";
#else
#error Unsupported Mac architecture
#endif
        NSBundle *bundle = NSBundle.mainBundle;
        NSString *contents = [bundle.bundlePath stringByAppendingPathComponent:@"Contents"];
        NSString *runtime = [contents stringByAppendingPathComponent:
            [NSString stringWithFormat:@"PlugIns/Runtime-%@.bundle/Contents/Home", arch]];
        NSString *native = [contents stringByAppendingPathComponent:
            [NSString stringWithFormat:@"Frameworks/Native/%@", arch]];
        NSString *modules = [contents stringByAppendingPathComponent:
            [NSString stringWithFormat:@"app/javafx/%@", arch]];
        // Foundation returns the application container when signed with App Sandbox.
        NSString *home = NSHomeDirectory();
        NSString *containerSuffix = [NSString stringWithFormat:@"/Library/Containers/%@/Data", bundle.bundleIdentifier];
        if (![home hasSuffix:containerSuffix]) {
            return fail(@"Der Mac-Store-Build muss mit App Sandbox signiert gestartet werden. Bitte die Mac-Anleitung verwenden.");
        }
        NSString *support = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory,
            NSUserDomainMask, YES) firstObject];
        NSString *data = [support stringByAppendingPathComponent:@"GarnBox"];
        if (support == nil || ![data hasPrefix:[home stringByAppendingString:@"/"]]) {
            return fail(@"Der geschützte Datenordner konnte nicht bestimmt werden.");
        }
        NSString *library = [runtime stringByAppendingPathComponent:@"lib/libjli.dylib"];
        void *handle = dlopen(library.fileSystemRepresentation, RTLD_NOW | RTLD_GLOBAL);
        if (!handle) {
            return fail([NSString stringWithFormat:@"Die eingebettete Java-Laufzeit konnte nicht geladen werden: %s", dlerror()]);
        }
        LaunchJava launch = (LaunchJava)dlsym(handle, "JLI_Launch");
        if (!launch) {
            return fail(@"Der Einstiegspunkt der eingebetteten Java-Laufzeit fehlt.");
        }
        NSMutableArray<NSString *> *arguments = [NSMutableArray arrayWithArray:@[
            [runtime stringByAppendingPathComponent:@"bin/java"],
            [@"-Djava.home=" stringByAppendingString:runtime],
            [@"-Djava.io.tmpdir=" stringByAppendingString:NSTemporaryDirectory()],
            @"-Dyarnbox.macAppStore=true",
            [@"-Dyarnbox.sandboxDataDirectory=" stringByAppendingString:data],
            [@"-Dorg.sqlite.lib.path=" stringByAppendingString:native],
            @"-Dorg.sqlite.lib.name=libsqlitejdbc.dylib",
            [@"-Djava.library.path=" stringByAppendingString:native],
            @"-Dfile.encoding=UTF-8", @"-Xdock:name=Yarn Box",
            [@"-Xdock:icon=" stringByAppendingString:[contents stringByAppendingPathComponent:@"Resources/icon.icns"]],
            @"--module-path", modules, @"--add-modules=javafx.controls",
            @"-cp", [contents stringByAppendingPathComponent:@"app/classpath/*"],
            @"de.appfabrik.garnbox.Launcher"
        ]];
        for (int index = 1; index < argc; index++) {
            NSString *argument = [NSString stringWithUTF8String:argv[index]];
            // Store installations always use the verified container, including test launches.
            if (!argument || [argument hasPrefix:@"--data-dir"]) {
                return fail(@"Dieser Store-Build verwendet ausschließlich seinen geschützten Datenordner.");
            }
            [arguments addObject:argument];
        }
        char **javaArgv = calloc(arguments.count + 1, sizeof(char *));
        if (!javaArgv) {
            return fail(@"Nicht genügend Arbeitsspeicher zum Starten der App.");
        }
        for (NSUInteger index = 0; index < arguments.count; index++) {
            javaArgv[index] = strdup(arguments[index].UTF8String);
            if (!javaArgv[index]) {
                for (NSUInteger previous = 0; previous < index; previous++) free(javaArgv[previous]);
                free(javaArgv);
                return fail(@"Nicht genügend Arbeitsspeicher zum Starten der App.");
            }
        }
        int result = launch((int)arguments.count, javaArgv, 0, NULL, 0, NULL,
            "21.0.13+9", "21.0", "java", "openjdk", 0, 1, 0, 0);
        for (NSUInteger index = 0; index < arguments.count; index++) free(javaArgv[index]);
        free(javaArgv);
        // The JVM can retain native threads until process termination. Do not dlclose it.
        return result;
    }
}
