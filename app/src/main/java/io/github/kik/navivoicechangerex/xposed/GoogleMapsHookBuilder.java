package io.github.kik.navivoicechangerex.xposed;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.common.collect.Iterables;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class GoogleMapsHookBuilder extends AbstactHookBuilder {
    public static final int NR_CLASSES = 100000;

    protected GoogleMapsHookBuilder(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        super(param);
    }

    private static String className(int index) {
        int n = 'z' - 'a' + 1;
        String s = "";
        while (index > 0) {
            int c = index % n;
            index /= n;
            s = (char) ('a' + c) + s;
        }
        return s;
    }

    @Override
    protected List<Class<?>> loadAllClasses() {
        var list = new ArrayList<Class<?>>();
        for (int i = 0; i < NR_CLASSES; i++) {
            try {
                list.add(classLoader().loadClass(className(i)));
            } catch (ClassNotFoundException ignore) {
            }
        }
        return list;
    }

    static class Specs {
        static Predicate<Class<?>> NetworkTtsQueueRunner() {
            // public final class NetworkTtsQueueRunner implements Runnable
            // {
            //      public NetworkTtsQueueRunner(
            //          PriorityBlockingQueue p0,
            //          Unknown1 p1,
            //          TtsTempManager p2,
            //          ApplicationParameters p3,
            //          Unknown2 p4,
            //          Executor p5,
            //          Executor p6,
            //          TtsStat p7,
            //          TtsSynthesizer p8,
            //          TtsSynthesizer p9,
            //          Unknown3 p10,
            //          NazoTriple p11) {
            ///     }
            // }
            return cls -> Stream.of(cls)
                    .filter(implementExact(Runnable.class))
                    .flatMap(GoogleMapsHookBuilder::getUniqueConstructor)
                    .filter(matchParam(0, PriorityBlockingQueue.class))
                    .filter(matchParam(5, Executor.class))
                    .filter(matchParam(6, Executor.class))
                    .anyMatch(matchParam(8, 9));
        }

        static Predicate<Class<?>> NetworkTtsQueueManager(NetworkTtsQueueRunnerContents c) {
            // public final class NetworkTtsQueueManager implements ???
            // {
            //      public NetworkTtsQueueManager(
            //          Unknown2 p0,
            //          ApplicationParameters p1,
            //          TtsStat p2,
            //          PriorityBlockingQueue p3,
            //          NetworkTtsQueueRunner p4,
            //          NazoTriple p5
            //      ) {
            //      }
            // }
            return cls -> Stream.of(cls)
                    .flatMap(GoogleMapsHookBuilder::getUniqueConstructor)
                    .filter(matchParam(0, c.Unknown2))
                    .filter(matchParam(1, c.ApplicationParameters))
                    .filter(matchParam(2, c.TtsStat))
                    .filter(matchParam(3, PriorityBlockingQueue.class))
                    .filter(matchParam(4, c.clazz))
                    .anyMatch(matchParam(5, c.NazoTriple));
        }

        static class NetworkTtsQueueRunnerContents {
            public final Class<?> clazz;
            public final Class<?> Unknown1;
            public final Class<?> TtsTempManager;
            public final Class<?> ApplicationParameters;
            public final Class<?> Unknown2;
            public final Class<?> TtsStat;
            public final Class<?> TtsSynthesizer;
            public final Class<?> Unknown3;
            public final Class<?> NazoTriple;
            public final Constructor<?> constructor;

            public NetworkTtsQueueRunnerContents(Class<?> cls) {
                this.clazz = cls;
                ModuleMain.module.log("class NetworkTtsQueueRunner = " + cls.getName());
                this.constructor = cls.getDeclaredConstructors()[0];
                ModuleMain.module.log("NetworkTtsQueueRunner.<init> = " + this.constructor);
                this.Unknown1 = this.constructor.getParameterTypes()[1];
                this.TtsTempManager = this.constructor.getParameterTypes()[2];
                this.ApplicationParameters = this.constructor.getParameterTypes()[3];
                this.Unknown2 = this.constructor.getParameterTypes()[4];
                this.TtsStat = this.constructor.getParameterTypes()[7];
                this.TtsSynthesizer = this.constructor.getParameterTypes()[8];
                this.Unknown3 = this.constructor.getParameterTypes()[10];
                this.NazoTriple = this.constructor.getParameterTypes()[11];
                ModuleMain.module.log("interface TtsSynthesizer = " + this.TtsSynthesizer);
            }
        }

        static class TtsSynthesizerContents {
            public final Method synthesizeToFile;

            public TtsSynthesizerContents(Class<?> cls) {
                // boolean TtsSynthesizer#synthesizeToFile(VoiceAlert alert, String path)
                this.synthesizeToFile = Arrays.stream(cls.getMethods())
                        .filter(method -> method.getParameterCount() == 2)
                        .filter(matchParam(1, String.class))
                        .findFirst().orElse(null);
                if (this.synthesizeToFile == null) {
                    ModuleMain.module.log("method synthesizeToFile not found");
                    return;
                }
                ModuleMain.module.log("method synthesizeToFile = " + this.synthesizeToFile);
            }
        }
    }

    public void run() {
        loadCache();
        runApplicationCapture();

        final Cached<Class<?>> clsNetworkTtsQueueRunner = findClass(
                "NetworkTtsQueueRunner",
                Specs.NetworkTtsQueueRunner());

        if (clsNetworkTtsQueueRunner.get() == null) {
            ModuleMain.module.log("NetworkTtsQueueRunner not found");
            return;
        }
        final var contentsNetworkTtsQueueRunner = new Specs.NetworkTtsQueueRunnerContents(clsNetworkTtsQueueRunner.get());

        ModuleMain.module.hook(contentsNetworkTtsQueueRunner.constructor, InspectHook.class);

        final var contentsTtsSynthesizer = new Specs.TtsSynthesizerContents(contentsNetworkTtsQueueRunner.TtsSynthesizer);

        final Cached<List<Class<?>>> TtsSynthesizerImpls = findClasses("TtsSynthesizerImpls",
                implementExact(contentsNetworkTtsQueueRunner.TtsSynthesizer));
        for (var impl : TtsSynthesizerImpls.get()) {
            ModuleMain.module.log("class implements TtsSynthesizer = " + impl);
            var method = getOverrideMethod(impl, contentsTtsSynthesizer.synthesizeToFile);
            if (method == null) {
                ModuleMain.module.log("synthesizeToFile not found: " + impl);
                continue;
            }
            ModuleMain.module.log("hook synthesizeToFile: " + method);
            ModuleMain.module.hook(method, SynthesizeHook.class);
        }

        final Cached<Class<?>> clsNetworkTtsQueueManager = findClass(
                "NetworkTtsQueueManager",
                Specs.NetworkTtsQueueManager(contentsNetworkTtsQueueRunner)
        );
        if (clsNetworkTtsQueueManager.get() == null) {
            ModuleMain.module.log("NetworkTtsQueueManager not found");
        }
        ModuleMain.module.log("NetworkTtsQueueManager = " + clsNetworkTtsQueueManager);

        final Method methodGetGuidanceText = Arrays.stream(clsNetworkTtsQueueManager.get().getDeclaredMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 3)
                .filter(matchParam(0, contentsNetworkTtsQueueRunner.ApplicationParameters))
                .filter(matchParam(2, contentsNetworkTtsQueueRunner.NazoTriple))
                .findFirst().orElse(null);

        if (methodGetGuidanceText == null) {
            ModuleMain.module.log("method getGuidanceText not found");
            return;
        }
        ModuleMain.module.log("getGuidanceText = " + methodGetGuidanceText);

        ModuleMain.module.hook(methodGetGuidanceText, SetVoiceNameHook.class);

        storeCache();
    }

    private void runApplicationCapture() {
        try {
            ModuleMain.applicationCaptureHookUnhooker = ModuleMain.module.hook(getMethod("android.content.ContextWrapper", "attachBaseContext"), ApplicationCaptureHook.class);
        } catch (Exception e) {
            ModuleMain.module.log("runApplicationCapture", e);
        }
    }

    private Method getMethod(String className, String methodName) throws Exception {
        var cls = classLoader().loadClass(className);
        var method = Arrays.stream(cls.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().orElse(null);
        return method;
    }


    @XposedHooker
    static class ApplicationCaptureHook implements XposedInterface.Hooker {
        @BeforeInvocation
        public static ApplicationCaptureHook beforeInvocation(XposedInterface.BeforeHookCallback callback) {
            ModuleMain.module.log("method " + callback.getMember() + " called with " + List.of(callback.getArgs()));
            return new ApplicationCaptureHook();
        }

        @AfterInvocation
        public static void afterInvocation(XposedInterface.AfterHookCallback callback, ApplicationCaptureHook context) {
            ModuleMain.module.log("method " + callback.getMember() + " return with " + callback.getResult());
            try {
                if (callback.getThisObject() instanceof Application) {
                    synchronized (ModuleMain.class) {
                        if (ModuleMain.module.application == null) {
                            ModuleMain.module.application = (Application) callback.getThisObject();
                            ModuleMain.applicationCaptureHookUnhooker.unhook();
                            ModuleMain.module.onApplicationCapture();
                        }
                    }
                }
            } catch (Exception e) {
                ModuleMain.module.log("application capture", e);
            }
        }
    }

    private static void dumpTextStructure(Object obj) {
        try {
            Field f = obj.getClass().getField("b");
            Iterable<Byte> structure = (Iterable<Byte>) f.get(obj);
            var os = new ByteArrayOutputStream();
            for (byte b : structure) {
                os.write(b);
            }
            os.close();
            var array = os.toByteArray();
            ModuleMain.module.log("\n" + Util.hexdump(array));
            ModuleMain.module.log("\n" + Util.dumpProto(array));
        } catch (Exception ignore) {
        }
    }

    @XposedHooker
    static class SynthesizeHook implements XposedInterface.Hooker {
        @BeforeInvocation
        public static SynthesizeHook beforeInvocation(XposedInterface.BeforeHookCallback callback) {
            ModuleMain.module.log("method " + callback.getMember() + " called with " + List.of(callback.getArgs()));
            dumpTextStructure(callback.getArgs()[0]);
            ModuleMain.Preferences p = ModuleMain.getPreferences();
            if (p.hookNetworkSynthesizer()) {
                if (p.disableNetworkSynthesizer()) {
                    callback.returnAndSkip(false);
                } else {
                    var api = p.getVoiceVoxEngine();
                    var p1 = callback.getArgs()[0];
                    var path = (String) callback.getArgs()[1];
                    boolean ret = false;
                    try {
                        Field f = p1.getClass().getField("a");
                        var text = (String) f.get(p1);
                        text = text.replaceAll(" ", "");
                        text = text.replaceAll("、、", "、");
                        String json = api.audio_query(p.voiceboxStyleId, text);
                        byte[] audio = api.synthesis(p.voiceboxStyleId, json);
                        try (var os = new FileOutputStream(path)) {
                            os.write(audio);
                        }
                        ret = true;
                    } catch (IOException ioe) {
                        ModuleMain.module.log("remote TTS failed", ioe);
                    } catch (Exception e) {
                        ModuleMain.module.log("hook parameter error", e);
                    }
                    callback.returnAndSkip(ret);
                }
            }
            return new SynthesizeHook();
        }

        @AfterInvocation
        public static void afterInvocation(XposedInterface.AfterHookCallback callback, SynthesizeHook context) {
            ModuleMain.module.log("method " + callback.getMember() + " return with " + callback.getResult());
        }
    }

    //
    // 音声合成キャッシュのIDの生成にボイス名をいれるようにする
    //
    @XposedHooker
    static class SetVoiceNameHook implements XposedInterface.Hooker {
        @BeforeInvocation
        public static SetVoiceNameHook beforeInvocation(XposedInterface.BeforeHookCallback callback) {
            ModuleMain.module.log("method " + callback.getMember() + " called with " + List.of(callback.getArgs()));
            return new SetVoiceNameHook();
        }

        @AfterInvocation
        public static void afterInvocation(XposedInterface.AfterHookCallback callback, SetVoiceNameHook context) {
            ModuleMain.Preferences p = ModuleMain.getPreferences();
            if (p.hookNetworkSynthesizer()) {
                // ネットワークTTSを使わないときは、キャッシュにヒットしないようなボイス名にしてなんとかする
                var voiceName = p.disableNetworkSynthesizer() ? "DISABLE-TTS" : "VOICEVOX-" + p.voiceboxStyleId;
                var ret = callback.getResult();
                Arrays.stream(ret.getClass().getDeclaredFields())
                        .filter(f -> f.getType().equals(String.class))
                        .forEach(f -> {
                            try {
                                f.setAccessible(true);
                                f.set(ret, voiceName);
                            } catch (IllegalAccessException e) {
                                ModuleMain.module.log("setting ret.voice failed", e);
                            }
                        });
            }
            ModuleMain.module.log("method " + callback.getMember() + " return with " + callback.getResult());
        }
    }
}
