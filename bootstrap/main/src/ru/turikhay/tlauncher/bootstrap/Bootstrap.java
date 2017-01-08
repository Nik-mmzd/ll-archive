package ru.turikhay.tlauncher.bootstrap;

import ru.turikhay.tlauncher.bootstrap.bridge.BootListenerAdapter;
import ru.turikhay.tlauncher.bootstrap.exception.ExceptionList;
import ru.turikhay.tlauncher.bootstrap.task.TaskInterruptedException;
import ru.turikhay.tlauncher.bootstrap.ui.UserInterface;
import shaded.com.getsentry.raven.DefaultRavenFactory;
import shaded.com.getsentry.raven.Raven;
import shaded.com.getsentry.raven.dsn.Dsn;
import shaded.com.getsentry.raven.event.Breadcrumb;
import shaded.com.getsentry.raven.event.BreadcrumbBuilder;
import shaded.com.getsentry.raven.event.Event;
import shaded.com.getsentry.raven.event.EventBuilder;
import shaded.com.getsentry.raven.event.interfaces.ExceptionInterface;
import ru.turikhay.tlauncher.bootstrap.bridge.BootListener;
import ru.turikhay.tlauncher.bootstrap.launcher.*;
import ru.turikhay.tlauncher.bootstrap.util.DataBuilder;
import shaded.joptsimple.ArgumentAcceptingOptionSpec;
import shaded.joptsimple.OptionParser;
import shaded.joptsimple.OptionSet;
import shaded.joptsimple.OptionSpecBuilder;
import ru.turikhay.tlauncher.bootstrap.bridge.BootBridge;
import ru.turikhay.tlauncher.bootstrap.json.Json;
import ru.turikhay.tlauncher.bootstrap.meta.*;
import ru.turikhay.tlauncher.bootstrap.task.Task;
import ru.turikhay.tlauncher.bootstrap.task.TaskList;
import ru.turikhay.tlauncher.bootstrap.util.U;
import ru.turikhay.tlauncher.bootstrap.util.stream.RedirectPrintStream;
import ru.turikhay.tlauncher.bootstrap.util.FileValueConverter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Bootstrap {
    private static final Raven raven = new DefaultRavenFactory().createRavenInstance(new Dsn("https://fe7b0410e04848019449cb8de9c9bc22:5c3a7bd40c9348dea0bc6858715570eb@sentry.ely.by/4?"));

    static Bootstrap createBootstrap() {
        log("Starting bootstrap...");

        Bootstrap bootstrap = new Bootstrap();
        LocalBootstrapMeta localBootstrapMeta = bootstrap.getMeta();

        log("Version: " + localBootstrapMeta.getVersion());

        File
            defaultFile = U.requireNotNull(LocalLauncher.getDefaultFileLocation(localBootstrapMeta.getShortBrand()), "defaultFileLocation"),
            defaultLibFolder = defaultFile.getParentFile() == null? new File("lib") : new File(defaultFile.getParentFile(), "lib");

        OptionParser parser = new OptionParser();
        ArgumentAcceptingOptionSpec<LaunchType> launchTypeParser =
                parser.accepts("launchType", "defines launch type").withRequiredArg().ofType(LaunchType.class).defaultsTo(U.requireNotNull(localBootstrapMeta.getLaunchType(), "default LaunchType"));
        ArgumentAcceptingOptionSpec<File> targetFileParser =
                parser.accepts("targetJar", "points to the targetJar").withRequiredArg().withValuesConvertedBy(new FileValueConverter()).defaultsTo(defaultFile);
        ArgumentAcceptingOptionSpec<File> targetLibFolderParser =
                parser.accepts("targetLibFolder", "points to the library folder").withRequiredArg().withValuesConvertedBy(new FileValueConverter()).defaultsTo(defaultLibFolder);
        ArgumentAcceptingOptionSpec<String> brandParser =
                parser.accepts("brand", "defines brand name").withRequiredArg().ofType(String.class).defaultsTo(U.requireNotNull(localBootstrapMeta.getShortBrand(), "default shortBrand"));
        OptionSpecBuilder forceUpdateParser =
                parser.accepts("forceUpdate", "defines if bootstrap should update launcher on update found");

        OptionSet parsed = parseJvmArgs(parser);

        localBootstrapMeta.setLaunchType(U.requireNotNull(launchTypeParser.value(parsed), "LaunchType"));
        log("Launch type:", localBootstrapMeta.getLaunchType());

        localBootstrapMeta.setShortBrand(U.requireNotNull(brandParser.value(parsed), "shortBrand"));
        log("Short brand: ", localBootstrapMeta.getShortBrand());

        localBootstrapMeta.setForceUpdate(parsed.has(forceUpdateParser) || localBootstrapMeta.isForceUpdate());
        log("Force update?", localBootstrapMeta.isForceUpdate());

        bootstrap.setTargetJar(targetFileParser.value(parsed));
        log("Target jar:", bootstrap.getTargetJar());

        bootstrap.setTargetLibFolder(targetLibFolderParser.value(parsed));
        log("Target lib folder:", bootstrap.getTargetLibFolder());

        recordBreadcrumb("createBootstrap", DataBuilder.create("localBootstrapMeta", localBootstrapMeta).add("targetJar", bootstrap.getTargetJar()).add("targetLibFolder", bootstrap.getTargetLibFolder()));
        return bootstrap;
    }

    private static OptionSet parseJvmArgs(OptionParser parser) {
        List<String> jvmArgs = new ArrayList<String>();

        for(String key : parser.recognizedOptions().keySet()) {
            String value = System.getProperty("tlauncher.bootstrap." + key);
            if(value != null) {
                jvmArgs.add("--" + key);
                jvmArgs.add(value);
            }
        }

        recordBreadcrumb("parseJvmArgs", DataBuilder.create("list", jvmArgs));

        return parser.parse(U.toArray(jvmArgs, String.class));
    }

    public static void main(String[] args) {
        checkRunningPath();

        System.setOut(out = RedirectPrintStream.newRedirectorFor(System.out));
        System.setErr(err = RedirectPrintStream.newRedirectorFor(System.err));

        Bootstrap bootstrap = null;

        try {
            bootstrap = createBootstrap();
            bootstrap.defTask(args).call();
        } catch(TaskInterruptedException interrupted) {
            log("Default task was interrupted");
        } catch (Exception e) {
            e.printStackTrace();

            LocalBootstrapMeta localBootstrapMeta = bootstrap == null? null : bootstrap.getMeta();
            raven.sendEvent(
                    new EventBuilder()
                            .withEnvironment(System.getProperty("os.name"))
                            .withLevel(Event.Level.FATAL)
                            .withSentryInterface(new ExceptionInterface(e))
                            .withRelease(localBootstrapMeta == null? null : String.valueOf(localBootstrapMeta.getVersion()))
            );

            if(bootstrap != null && bootstrap.getUserInterface() != null) {
                bootstrap.getUserInterface().getFrame().dispose();
            }

            UserInterface.showError("Could not start TLauncher!", RedirectPrintStream.getBuffer().toString());
            System.exit(-1);
        }

        System.exit(0);
    }

    private final UserInterface ui;
    private final InternalLauncher internal;
    private final LocalBootstrapMeta meta;
    private File targetJar, targetLibFolder;

    Bootstrap(File targetJar, File targetLibFolder) {
        UserInterface userInterface;
        try {
            userInterface = new UserInterface();
        } catch(RuntimeException rE) {
            log("User interface is not loaded:", rE);
            userInterface = null;
        }
        this.ui = userInterface;

        final String resourceName = "meta.json";
        try {
            meta = Json.parse(U.requireNotNull(getClass().getResourceAsStream(resourceName), resourceName), LocalBootstrapMeta.class);
        } catch (Exception e) {
            throw new Error("could not load meta", e);
        }

        InternalLauncher internal;
        try {
            internal = new InternalLauncher();
        } catch (LauncherNotFoundException e) {
            log("Could not locate internal launcher", e);
            internal = null;
        }
        this.internal = internal;

        setTargetJar(targetJar);
        setTargetLibFolder(targetLibFolder);

        recordBreadcrumb("initBootstrap", new DataBuilder().add("ui", ui).add("internalLauncher", internal == null? null : internal.toString()).add("targetJar", targetJar).add("targetLibFolder", targetLibFolder));
    }

    public Bootstrap() {
        this(null, null);
    }

    UserInterface getUserInterface() {
        return ui;
    }

    public File getTargetJar() {
        return targetJar;
    }

    private void setTargetJar(File file) {
        this.targetJar = file;
    }

    public File getTargetLibFolder() {
        return targetLibFolder;
    }

    private void setTargetLibFolder(File targetLibFolder) {
        this.targetLibFolder = targetLibFolder;
    }

    public LocalBootstrapMeta getMeta() {
        return meta;
    }

    DownloadEntry getBootstrapUpdate(UpdateMeta updateMeta) {
        RemoteBootstrapMeta remoteMeta = U.requireNotNull(updateMeta, "updateMeta").getBootstrap();

        U.requireNotNull(remoteMeta, "RemoteBootstrap meta");
        U.requireNotNull(remoteMeta.getDownload(), "RemoteBootstrap download URL");

        log("Local bootstrap version: " + meta.getVersion());
        log("Remote bootstrap version: " + remoteMeta.getVersion());

        String localBootstrapChecksum;
        try {
            localBootstrapChecksum = U.getSHA256(U.getJar(Bootstrap.class));
        } catch (Exception e) {
            log("Could not get local bootstrap checksum", e);
            return null;
        }

        log("Current package: " + PackageType.CURRENT);
        log("Remote bootstrap checksum of selected package: " + remoteMeta.getDownload(PackageType.CURRENT));

        log("Local bootstrap checksum: " + localBootstrapChecksum);
        log("Remote bootstrap checksum: " + remoteMeta.getDownload(PackageType.CURRENT).getChecksum());

        if (localBootstrapChecksum.equalsIgnoreCase(remoteMeta.getDownload().getChecksum())) {
            return null;
        }

        recordBreadcrumb("getBootstrapUpdate", null);
        return remoteMeta.getDownload();
    }

    TaskList downloadLibraries(LocalLauncherMeta localLauncherMeta) {
        TaskList taskList = new TaskList("downloadLibraries", 4);
        File libDir = getTargetLibFolder();

        for (Library library : localLauncherMeta.getLibraries()) {
            taskList.submit(library.download(libDir));
        }

        recordBreadcrumb("downloadLibraries", DataBuilder.create("taskList", taskList));
        return taskList;
    }

    private BootBridge createBridge(String[] args, String options) {
        BootBridge bridge = BootBridge.create(meta.getVersion().toString(), args, options);
        bridge.addListener(new BootListenerAdapter() {
            @Override
            public void onBootSucceeded() {
                disableRedirectRecording();
            }
        });
        List<String> argsList = new ArrayList<String>();
        Collections.addAll(argsList, args);
        recordBreadcrumb("createBridge", DataBuilder.create("args", argsList.toString()).add("options", options));
        return bridge;
    }

    Task<BootBridge> bootLauncher(final UpdateMeta updateMeta, final String[] args) {
        U.requireNotNull(args, "args");

        return new Task<BootBridge>("bootLauncher") {
            @Override
            protected BootBridge execute() throws Exception {
                final double start = .75, end = 1., delta = end - start;

                RemoteLauncher remoteLauncher = updateMeta == null? null : new RemoteLauncher(updateMeta.getLauncher());
                log("Remote launcher: " + remoteLauncher);
                recordBreadcrumb("remoteLauncher", DataBuilder.create("value", String.valueOf(remoteLauncher)));

                LocalLauncher localLauncher = bindTo(getLocalLauncher(remoteLauncher), .0, .25);
                LocalLauncherMeta localLauncherMeta = localLauncher.getMeta();
                log("Local launcher: " + localLauncher);
                recordBreadcrumb("localLauncher", DataBuilder.create("value", String.valueOf(remoteLauncher)));

                log("Downloading libraries...");
                bindTo(downloadLibraries(localLauncherMeta), .25, start);

                BootBridge bridge = createBridge(args, updateMeta == null? null : updateMeta.getOptions());

                log("Starting launcher...");
                recordBreadcrumb("startingLauncher", null);
                bridge.addListener(new BootListenerAdapter() {
                    @Override
                    public void onBootStateChanged(String stepName, double percentage) {
                        updateProgress(start + delta * percentage);
                    }
                });
                bindTo(meta.getLaunchType().getStarter().start(localLauncher, bridge), start, end);

                return bridge;
            }
        };
    }

    private Task<Void> defTask(final String[] args) {
        return new Task<Void>("defTask") {
            {
                if(ui != null) {
                    ui.bindToTask(this);
                }
            }

            @Override
            protected Void execute() throws Exception {
                UpdateMeta updateMeta;

                try {
                    updateMeta = bindTo(UpdateMeta.fetchFor(meta.getShortBrand()), .0, .25);
                } catch(ExceptionList list) {
                    log("Could not retrieve update meta:", list);
                    updateMeta = null;
                }

                if(updateMeta != null) {
                    DownloadEntry downloadEntry = getBootstrapUpdate(updateMeta);
                    if(downloadEntry != null) {
                        Updater updater = new Updater("bootstrapUpdate", U.getJar(Bootstrap.class), downloadEntry, true);
                        bindTo(updater, .25, 1.);
                        return null;
                    }
                }

                BootBridge bridge = bindTo(bootLauncher(updateMeta, args), .25, 1.);

                checkInterrupted();

                log("Idle state: Waiting for launcher the close");
                bridge.waitUntilClose();

                return null;
            }
        };
    }

    private Task<LocalLauncher> getLocalLauncher(final RemoteLauncher remote) {
        return new Task<LocalLauncher>("getLocalLauncher") {
            @Override
            protected LocalLauncher execute() throws Exception {
                updateProgress(0.);
                log("Getting local launcher...");

                RemoteLauncherMeta remoteLauncherMeta = remote == null? null : U.requireNotNull(remote.getMeta(), "RemoteLauncherMeta");

                LocalLauncher local;
                try {
                    local = new LocalLauncher(getTargetJar(), getTargetLibFolder());
                } catch (LauncherNotFoundException lnfE) {
                    log("Could not find local launcher:", lnfE);

                    if (internal == null) {
                        log("... and we have no internal one?");
                        local = null;
                    } else {
                        log("... replacing it with internal one:", internal);
                        local = bindTo(internal.toLocalLauncher(getTargetJar(), getTargetLibFolder()), .0, .1);
                    }
                }

                File file = local != null? local.getFile() : getTargetJar();

                if(local != null) {
                    if(remote == null) {
                        log("We have local launcher, but have no remote.");
                        return local;
                    }

                    LocalLauncherMeta localLauncherMeta;

                    try {
                        localLauncherMeta = U.requireNotNull(local.getMeta(), "LocalLauncherMeta");
                    } catch (IOException ioE) {
                        log("Could not get local launcher meta:", ioE);
                        localLauncherMeta = null;
                    }

                    updateProgress(.2);

                    replaceSelect:
                    {
                        if (localLauncherMeta == null) {
                            break replaceSelect;
                        }

                        U.requireNotNull(localLauncherMeta.getShortBrand(), "LocalLauncher shortBrand");
                        U.requireNotNull(localLauncherMeta.getBrand(), "LocalLauncher brand");
                        U.requireNotNull(localLauncherMeta.getMainClass(), "LocalLauncher mainClass");

                        String localLauncherHash = U.getSHA256(local.getFile());
                        log("Local SHA256: " + localLauncherHash);
                        log("Remote SHA256: " + remoteLauncherMeta.getChecksum());

                        if (!localLauncherHash.equalsIgnoreCase(remoteLauncherMeta.getChecksum())) {
                            log("... local SHA256 checksum is not the same as remote");
                            break replaceSelect;
                        }

                        log("All done, local launcher is up to date.");

                        return local;
                    }

                    updateProgress(.5);
                }

                if(remote == null) {
                    throw new LauncherNotFoundException("could not retrieve any launcher");
                }

                return bindTo(remote.toLocalLauncher(file, getTargetLibFolder()), .5, 1.);
            }
        };
    }

    public static void recordBreadcrumbError(Class<?> clazz, String name, Throwable t, DataBuilder b) {
        recordBreadcrumb(name, "error", "class:" + clazz.getSimpleName(), b.add("exception", U.toString(t)));
    }

    public static void recordBreadcrumb(Class<?> clazz, String name, DataBuilder data) {
        recordBreadcrumb(name, "info", "class:" + clazz.getSimpleName(), data);
    }

    private static void recordBreadcrumb(String name, DataBuilder data) {
        recordBreadcrumb(name, "info", "general", data);
    }

    private static void recordBreadcrumb(String name, String level, String category, DataBuilder data) {
        BreadcrumbBuilder b = new BreadcrumbBuilder();
        b.setLevel(level);
        b.setCategory(category);
        b.setMessage(name);
        if(data != null) {
            b.setData(data.build());
        }

        Breadcrumb breadcrumb = b.build();
        log("Added breadcrumb:", breadcrumb);
        raven.getContext().recordBreadcrumb(breadcrumb);
    }

    private static RedirectPrintStream.Redirector out, err;

    private static void disableRedirectRecording() {
        if (out != null) {
            out.disableRecording();
        }
        if (err != null) {
            err.disableRecording();
        }
        recordBreadcrumb("disableRedirectRecording", null);
    }

    private static void checkRunningPath() {
        String path = U.getJar(Bootstrap.class).getAbsolutePath();

        if (path.contains("!")) {
            String message =
                    "Please do not run (any) Java application which path contains folder name that ends with «!»" +
                            "\n" +
                            "Не запускайте Java-приложения в директориях, чей путь содержит «!». Переместите TLauncher в другую папку." +
                            "\n\n" + path;
            UserInterface.showError(message, null);
            throw new Error(message);
        }
    }

    private static void log(Object... o) {
        U.log("[Bootstrap]", o);
    }
}
