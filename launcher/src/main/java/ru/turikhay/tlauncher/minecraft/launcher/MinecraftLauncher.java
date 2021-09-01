package ru.turikhay.tlauncher.minecraft.launcher;

import com.google.gson.Gson;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import net.minecraft.launcher.process.JavaProcess;
import net.minecraft.launcher.process.JavaProcessLauncher;
import net.minecraft.launcher.process.JavaProcessListener;
import net.minecraft.launcher.process.PrintStreamType;
import net.minecraft.launcher.updater.AssetIndex;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.*;
import net.minecraft.launcher.versions.json.DateTypeAdapter;
import net.minecraft.options.OptionsFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import ru.turikhay.app.nstweaker.NSTweaker;
import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.configuration.Configuration;
import ru.turikhay.tlauncher.downloader.AbortedDownloadException;
import ru.turikhay.tlauncher.downloader.DownloadableContainer;
import ru.turikhay.tlauncher.downloader.Downloader;
import ru.turikhay.tlauncher.jre.JavaPlatform;
import ru.turikhay.tlauncher.jre.JavaRuntimeLocal;
import ru.turikhay.tlauncher.jre.JavaRuntimeRemote;
import ru.turikhay.tlauncher.managers.*;
import ru.turikhay.tlauncher.minecraft.*;
import ru.turikhay.tlauncher.minecraft.auth.Account;
import ru.turikhay.tlauncher.minecraft.crash.CrashManager;
import ru.turikhay.tlauncher.stats.Stats;
import ru.turikhay.tlauncher.ui.alert.Alert;
import ru.turikhay.tlauncher.ui.loc.Localizable;
import ru.turikhay.tlauncher.user.PlainUser;
import ru.turikhay.util.*;
import ru.turikhay.util.async.AsyncThread;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MinecraftLauncher implements JavaProcessListener {
    private static final Logger LOGGER = LogManager.getLogger(MinecraftLauncher.class);

    public static final String SENTRY_CONTEXT_NAME = "minecraftLauncher", CAPABLE_WITH = "1.6.84-j";
    private static final int OFFICIAL_VERSION = 21, ALTERNATIVE_VERSION = 13, MIN_WORK_TIME = 5000;
    private boolean working;
    private boolean killed;
    private final Thread parentThread;
    private final Gson gson;
    private final DateTypeAdapter dateAdapter;
    private final Downloader downloader;
    private final Configuration settings;
    private final boolean forceUpdate;
    private final boolean assistLaunch;
    private final VersionManager vm;
    private final AssetsManager am;
    private final ProfileManager pm;
    private CrashManager crashManager;
    private final List<MinecraftListener> listeners;
    private final List<MinecraftExtendedListener> extListeners;
    private final List<MinecraftLauncherAssistant> assistants;
    private MinecraftLauncher.MinecraftLauncherStep step;
    private Account.AccountType librariesForType;
    private String oldMainclass;
    private String versionName;
    private VersionSyncInfo versionSync;
    private CompleteVersion version;
    private CompleteVersion deJureVersion;
    private boolean isLauncher;
    private String accountName;
    private Account account;
    private String family;
    private File rootDir;
    private File gameDir;
    private File localAssetsDir;
    private File nativeDir;
    private File globalAssetsDir;
    private File assetsIndexesDir;
    private File assetsObjectsDir;
    private int[] windowSize;
    private boolean fullScreen;
    private boolean fullCommand;
    private int ramSize;
    private OptionsFile optionsFile;
    private JavaProcessLauncher launcher;
    private String programArgs;
    private boolean minecraftWorking;
    private long startupTime;
    private int exitCode;
    private Server server;
    private List<PromotedServer> promotedServers;
    private List<PromotedServer> outdatedPromotedServers;
    private PromotedServerAddStatus promotedServerAddStatus = PromotedServerAddStatus.NONE;
    private int serverId;
    private static boolean ASSETS_WARNING_SHOWN;
    private JavaProcess process;

    private final Rule.FeatureMatcher featureMatcher = createFeatureMatcher();

    private ChildProcessLogger processLogger;

    public ChildProcessLogger getProcessLogger() {
        return processLogger;
    }

    private Charset charset;

    public Charset getCharset() {
        return charset;
    }

    private JavaManager javaManager;

    private JavaManagerConfig javaManagerConfig;

    private JavaManagerConfig.JreType jreType;

    public JavaManagerConfig.JreType getJreType() {
        return jreType;
    }

    private String jreExec;

    public Downloader getDownloader() {
        return downloader;
    }

    public Configuration getConfiguration() {
        return settings;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public boolean isLaunchAssist() {
        return assistLaunch;
    }

    public MinecraftLauncher.MinecraftLauncherStep getStep() {
        return step;
    }

    public boolean isWorking() {
        return working;
    }

    public boolean isMinecraftRunning() {
        return working && minecraftWorking && !killed;
    }

    private MinecraftLauncher(ComponentManager manager, Downloader downloader, Configuration configuration, boolean forceUpdate, boolean exit) {
        if (manager == null) {
            throw new NullPointerException("Ti sovsem s duba ruhnul?");
        } else if (downloader == null) {
            throw new NullPointerException("Downloader is NULL!");
        } else if (configuration == null) {
            throw new NullPointerException("Configuration is NULL!");
        } else {
            parentThread = Thread.currentThread();
            gson = new Gson();
            dateAdapter = new DateTypeAdapter(true);
            this.downloader = downloader;
            settings = configuration;
            assistants = manager.getComponentsOf(MinecraftLauncherAssistant.class);
            vm = manager.getComponent(VersionManager.class);
            am = manager.getComponent(AssetsManager.class);
            pm = manager.getComponent(ProfileManager.class);
            javaManager = TLauncher.getInstance().getJavaManager();
            this.forceUpdate = forceUpdate;
            assistLaunch = !exit;


            listeners = Collections.synchronizedList(new ArrayList<>());
            extListeners = Collections.synchronizedList(new ArrayList<>());
            step = MinecraftLauncher.MinecraftLauncherStep.NONE;

            LOGGER.info("Alternative Minecraft Launcher ({}) has initialized", ALTERNATIVE_VERSION);
            LOGGER.info("Compatible with official version: {}", OFFICIAL_VERSION);
        }
    }

    public MinecraftLauncher(TLauncher t, boolean forceUpdate) {
        this(t.getManager(), t.getDownloader(), t.getSettings(), forceUpdate, t.getSettings().getActionOnLaunch() == Configuration.ActionOnLaunch.EXIT);
    }

    public void addListener(MinecraftListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        } else {
            if (listener instanceof MinecraftExtendedListener) {
                extListeners.add((MinecraftExtendedListener) listener);
            }

            listeners.add(listener);
        }
    }

    public void start() {
        checkWorking();
        working = true;

        try {
            collectInfo();
        } catch (Throwable var5) {
            LOGGER.error("Caught an exception", var5);
            if (var5 instanceof MinecraftException) {
                MinecraftException listener2 = (MinecraftException) var5;

                for (MinecraftListener listener3 : listeners) {
                    listener3.onMinecraftKnownError(listener2);
                }
            } else {
                MinecraftListener listener;
                Iterator<MinecraftListener> listener1;
                if (var5 instanceof MinecraftLauncher.MinecraftLauncherAborted) {
                    listener1 = listeners.iterator();

                    while (listener1.hasNext()) {
                        listener = listener1.next();
                        listener.onMinecraftAbort();
                    }
                } else {
                    Sentry.capture(new EventBuilder()
                            .withMessage("minecraft launcher exception")
                            .withSentryInterface(new ExceptionInterface(var5))
                            .withLevel(Event.Level.ERROR)
                    );

                    listener1 = listeners.iterator();

                    while (listener1.hasNext()) {
                        listener = listener1.next();
                        listener.onMinecraftError(var5);
                    }
                }
            }
        }

        working = false;
        step = MinecraftLauncher.MinecraftLauncherStep.NONE;
        LOGGER.info("Launcher has stopped.");
    }

    public void stop() {
        if (step == MinecraftLauncher.MinecraftLauncherStep.NONE) {
            throw new IllegalStateException();
        } else {
            if (step == MinecraftLauncher.MinecraftLauncherStep.DOWNLOADING) {
                downloader.stopDownload();
            }

            working = false;
        }
    }

    public String getVersion() {
        return version.getID();
    }

    public CompleteVersion getCompleteVersion() {
        return version;
    }

    public void setVersion(String name) {
        checkWorking();
        this.versionName = name;
    }

    public int getExitCode() {
        return exitCode;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server, int id) {
        checkWorking();
        this.server = server;
        this.serverId = id;
    }

    public void setPromotedServers(List<PromotedServer> serverList, List<PromotedServer> outdatedServerList) {
        this.promotedServers = U.shuffle(serverList);
        this.outdatedPromotedServers = outdatedServerList;
    }

    public OptionsFile getOptionsFile() {
        return optionsFile;
    }

    private void collectInfo() throws MinecraftException {
        checkStep(MinecraftLauncher.MinecraftLauncherStep.NONE, MinecraftLauncher.MinecraftLauncherStep.COLLECTING);
        LOGGER.info("Collecting info...");

        for (MinecraftListener type : listeners) {
            type.onMinecraftPrepare();
        }

        for (MinecraftExtendedListener type1 : extListeners) {
            type1.onMinecraftCollecting();
        }

        LOGGER.info("Is force updating: {}", forceUpdate);


        if (versionName == null) {
            versionName = settings.get("login.version");
        }
        if (versionName == null || versionName.isEmpty()) {
            throw new IllegalArgumentException("Version name is NULL or empty!");
        }

        LOGGER.info("Version id: {}", versionName);


        versionSync = vm.getVersionSyncInfo(versionName);
        if (versionSync == null) {
            throw new IllegalArgumentException("Cannot find version " + versionName);
        }

        LOGGER.debug("Version sync info: {}", versionSync);

        try {
            deJureVersion = versionSync.resolveCompleteVersion(vm, forceUpdate);
        } catch (IOException e) {
            throw new MinecraftException(false, "Can't resolve version", "could-not-fetch-complete-version");
        }

        try {
            deJureVersion.validate();
        } catch(RuntimeException rE) {
            throw new RuntimeException("Invalid version", rE);
        }

        if (deJureVersion.getReleaseType() == ReleaseType.LAUNCHER) {
            isLauncher = true;
        }

        accountName = settings.get("login.account");
        if (accountName != null && !accountName.isEmpty()) {
            Account.AccountType type2 = Reflect.parseEnum(Account.AccountType.class, settings.get("login.account.type"));
            account = pm.getAuthDatabase().getByUsername(accountName, type2);
        }
        if(account == null) {
            if (isLauncher) {
                LOGGER.debug("Account is not required, setting user \"launcher\"");
                accountName = "launcher";
                account = new Account(new PlainUser("launcher", new UUID(0L, 0L)));
            } else {
                throw new NullPointerException("account");
            }
        }

        LOGGER.info("Selected account: {}", account.getUser().getDisplayName());
        LOGGER.debug("Account info: {}", account);

        if (!isLauncher) {
            Account.AccountType lookupLibrariesForType;
            switch (account.getType()) {
                case MCLEAKS:
                    if (McleaksManager.isUnsupported()) {
                        throw new MinecraftException(false, "MCLeaks is not supported", "mcleaks-unsupported");
                    } else {
                        lookupLibrariesForType = Account.AccountType.MCLEAKS;
                        oldMainclass = deJureVersion.getMainClass();
                    }
                    break;
                case ELY:
                case ELY_LEGACY:
                    lookupLibrariesForType = Account.AccountType.ELY;
                    break;
                case PLAIN:
                    mayBeEly:
                    {
                        if (!TLauncher.getInstance().getLibraryManager().isAllowElyEverywhere()) {
                            break mayBeEly;
                        }
                        if (!settings.getBoolean("ely.globally")) {
                            break mayBeEly;
                        }
                        lookupLibrariesForType = Account.AccountType.ELY;
                        break;
                    }
                    lookupLibrariesForType = Account.AccountType.PLAIN;
                    break;
                default:
                    lookupLibrariesForType = account.getType();
            }

            LOGGER.debug("Looking up replacement libraries for {}", librariesForType = lookupLibrariesForType);

            TLauncher.getInstance().getLibraryManager().refreshComponent();
            if (TLauncher.getInstance().getLibraryManager().hasLibraries(deJureVersion, librariesForType)) {

                version = TLauncher.getInstance().getLibraryManager().process(deJureVersion, librariesForType);
                LOGGER.info("Some libraries will be replaced");
            } else {

                version = deJureVersion;
                LOGGER.info("No library will be replaced");
            }
        } else {
            version = deJureVersion;
        }


        LOGGER.trace("Version: {}", version);

        family = version.getFamily();
        if (StringUtils.isEmpty(family))
            family = "unknown";
        LOGGER.debug("Family: {}", family);

        javaManagerConfig = settings.get(JavaManagerConfig.class);
        jreType = javaManagerConfig.getJreTypeOrDefault();

        rootDir = new File(settings.get("minecraft.gamedir"));


        long freeSpace = rootDir.getUsableSpace();
        if (freeSpace > 0 && freeSpace < 1024L * 64L) {
            throw new MinecraftException(true, "Insufficient space " + rootDir.getAbsolutePath() + "(" + freeSpace + ")", "free-space", rootDir);
        }

        if (settings.getBoolean("minecraft.gamedir.separate")) {
            gameDir = new File(rootDir, "home/" + family);
        } else {
            gameDir = rootDir;
        }


        detectCharsetOnWindows();

        if(charset == null) {
            charset = StandardCharsets.UTF_8;
            LOGGER.info("Using standard charset: {}", charset);
        }

        try {
            FileUtil.createFolder(rootDir);
        } catch (Exception var9) {
            throw new MinecraftException(true, "Cannot create working directory!", "folder-not-found", var9);
        }
        if (!isLauncher) {
            try {
                FileUtil.createFolder(gameDir);
            } catch (Exception var9) {
                throw new MinecraftException(true, "Cannot create game directory!", "folder-not-found", var9);
            }
        }

        LOGGER.info("Root directory: {}", rootDir);
        LOGGER.info("Game directory: {}", gameDir);

        optionsFile = new OptionsFile(new File(gameDir, "options.txt"));

        if (optionsFile.getFile().isFile()) {
            try {
                optionsFile.read();
            } catch (IOException ioE) {

                LOGGER.warn("Could not read options file {}", optionsFile.getFile(), ioE);
            }
        }

        LOGGER.info("Options: {}", optionsFile);


        globalAssetsDir = new File(rootDir, "assets");

        if (!isLauncher) {
            LOGGER.trace("Global assets dir: {}", globalAssetsDir);
            try {
                FileUtil.createFolder(globalAssetsDir);
            } catch (IOException var8) {
                throw new RuntimeException("Cannot create assets directory!", var8);
            }
        }

        assetsIndexesDir = new File(globalAssetsDir, "indexes");

        if (!isLauncher) {
            LOGGER.trace("Assets indexes dir: {}", assetsIndexesDir);
            try {
                FileUtil.createFolder(assetsIndexesDir);
            } catch (IOException var7) {
                throw new RuntimeException("Cannot create assets indexes directory!", var7);
            }
        }

        assetsObjectsDir = new File(globalAssetsDir, "objects");

        if (!isLauncher) {
            LOGGER.trace("Asset objects dir: {}", assetsIndexesDir);
            try {
                FileUtil.createFolder(assetsObjectsDir);
            } catch (IOException var6) {
                throw new RuntimeException("Cannot create assets objects directory!", var6);
            }
        }

        nativeDir = new File(rootDir, "versions/" + version.getID() + "/" + "natives");
        LOGGER.trace("Natives dir: {}", nativeDir);
        try {
            FileUtil.createFolder(nativeDir);
        } catch (IOException var5) {
            throw new RuntimeException("Cannot create native files directory!", var5);
        }

        programArgs = settings.get("minecraft.args");
        if (programArgs != null && programArgs.isEmpty()) {
            programArgs = null;
        }

        LOGGER.trace("Args: {}", programArgs);

        windowSize = settings.getClientWindowSize();
        LOGGER.trace("Window size: {}", windowSize);
        if (windowSize[0] < 1) {
            throw new IllegalArgumentException("Invalid window width!");
        } else if (windowSize[1] < 1) {
            throw new IllegalArgumentException("Invalid window height!");
        } else {
            fullScreen = settings.getBoolean("minecraft.fullscreen");


            ramSize = settings.getInteger("minecraft.memory");
            if (ramSize < 512) {
                throw new IllegalArgumentException("Invalid RAM size!");
            }


            fullCommand = settings.getBoolean("gui.logger.fullcommand");


            for (MinecraftLauncherAssistant assistant : assistants) {
                assistant.collectInfo();
            }

            LOGGER.info("Checking conditions...");
            if (version.getMinimumCustomLauncherVersion() > ALTERNATIVE_VERSION) {
                throw new MinecraftException(false, "Alternative launcher is incompatible with launching version!", "incompatible");
            } else {
                if (version.getMinimumCustomLauncherVersion() == 0 && version.getMinimumLauncherVersion() > OFFICIAL_VERSION) {
                    LOGGER.warn("Required launcher version is newer: {} > {}", version.getMinimumLauncherVersion(), OFFICIAL_VERSION);
                    Alert.showLocWarning("launcher.warning.title", "launcher.warning.incompatible.launcher", null);
                }

                if (!version.appliesToCurrentEnvironment(featureMatcher)) {
                    Alert.showLocWarning("launcher.warning.title", "launcher.warning.incompatible.environment", null);
                }

                downloadResources();
            }
        }
    }

    private void detectCharsetOnWindows() {
        if(!OS.WINDOWS.isCurrent()) {
            return;
        }
        if(StringUtils.isAsciiPrintable(gameDir.getAbsolutePath())) {
            LOGGER.debug("Path to the game directory only contains ASCII characters.");
            LOGGER.debug("I reckon it's fine to use standard UTF-8");
            return;
        }
        String systemCharsetName = System.getProperty("tlauncher.systemCharset");
        if(systemCharsetName == null) {
            LOGGER.warn("System charset is unknown");
            detectUsingCharsetDetectTool();
        } else {
            useSystemCharsetFromSysProp(systemCharsetName);
        }
    }

    private void useSystemCharsetFromSysProp(String systemCharsetName) {
        Charset charset;
        try {
            charset = Charset.forName(systemCharsetName);
        } catch(RuntimeException rE) {
            LOGGER.warn("Couldn't find charset {}. It was passed as a system charset.", systemCharsetName, rE);
            Sentry.capture(new EventBuilder()
                    .withLevel(Event.Level.ERROR)
                    .withMessage("couldn't find system charset \"" + systemCharsetName + "\"")
                    .withSentryInterface(new ExceptionInterface(rE))
            );
            return;
        }
        LOGGER.debug("Using system charset from system properties: {}", charset.name());
        this.charset = charset;
    }

    private void detectUsingCharsetDetectTool() {
        Charset charset;
        try {
            charset = AsyncThread.future(() -> CharsetDetect.detect(OS.getJavaPath())).get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.warn("Couldn't detect system charset using {} tool",
                    CharsetDetect.class.getSimpleName(), e);
            Sentry.capture(new EventBuilder()
                    .withLevel(Event.Level.ERROR)
                    .withMessage("couldn't detect system charset")
                    .withSentryInterface(new ExceptionInterface(e))
            );
            return;
        } catch (InterruptedException interruptedException) {
            throw new MinecraftLauncherAborted(interruptedException);
        }
        LOGGER.debug("Detected system charset: {}", charset);
        this.charset = charset;
    }

    public File getRootDir() {
        return rootDir;
    }

    public File getGameDir() {
        return gameDir;
    }

    private void downloadResources() throws MinecraftException {
        checkStep(MinecraftLauncher.MinecraftLauncherStep.COLLECTING, MinecraftLauncher.MinecraftLauncherStep.DOWNLOADING);

        boolean fastCompare;
        if (versionSync.isInstalled()) {
            fastCompare = !forceUpdate;
        } else {
            fastCompare = false;
        }

        for (MinecraftExtendedListener assets : extListeners) {
            assets.onMinecraftComparingAssets(fastCompare);
        }

        final List<AssetIndex.AssetObject> assets1 = compareAssets(fastCompare);

        DownloadableContainer jreContainer = null;
        if(jreType instanceof JavaManagerConfig.Recommended) {
            CompleteVersion.JavaVersion javaVersion = version.getJavaVersion();
            if(javaVersion == null) {
                LOGGER.debug("Current Minecraft version doesn't have JRE requirements");
                javaVersion = javaManager.getFallbackRecommendedVersion(version, true);
                if(javaVersion != null) {
                    LOGGER.debug("Will use fallback recommended version: {}", javaVersion);
                }
            }
            if(JavaPlatform.CURRENT_PLATFORM == null) {
                LOGGER.warn("Current platform is unsupported");
                jreType = new JavaManagerConfig.Current();
                Alert.showWarning("", Localizable.get("launcher.warning.jre-platform-unsupported"));
            } else if(javaVersion == null) {
                jreType = new JavaManagerConfig.Current();
            } else {
                String jreName = javaVersion.getComponent();
                LOGGER.debug("Will use JRE: {}", javaVersion);
                Optional<JavaRuntimeLocal> latestLocalOpt;
                try {
                    latestLocalOpt = javaManager.getLatestVersionInstalled(jreName);
                } catch (InterruptedException interruptedException) {
                    throw new MinecraftLauncherAborted(interruptedException);
                }
                // reinstall JRE if forceUpdate is checked, but ignore it if version has override
                if(latestLocalOpt.isPresent() && (latestLocalOpt.get().hasOverride() || !forceUpdate)) {
                    LOGGER.debug("Latest version of required JRE is installed");
                    jreExec = latestLocalOpt.get().getExecutableFile().getAbsolutePath();
                } else {
                    LOGGER.debug("Will install required JRE");
                    Optional<JavaRuntimeRemote> remoteRuntimeOpt;
                    try {
                        remoteRuntimeOpt = javaManager.getFetcher().fetchNow()
                                .getCurrentPlatformLatestRuntime(jreName);
                    } catch (ExecutionException e) {
                        LOGGER.error("Couldn't fetch remote JRE list", e);
                        remoteRuntimeOpt = Optional.empty();
                    } catch (InterruptedException interruptedException) {
                        throw new MinecraftLauncherAborted(interruptedException);
                    }
                    if(remoteRuntimeOpt.isPresent()) {
                        JavaRuntimeRemote remoteRuntime = remoteRuntimeOpt.get();
                        File javaRootDir = javaManager.getDiscoverer().getRootDir();
                        try {
                            if (!javaManager.hasEnoughSpaceToInstall(remoteRuntime)) {
                                boolean continueWithoutInstallation = Alert.showQuestion(
                                        "",
                                        Localizable.get("launcher.warning.jre-will-take-remaining-space",
                                                remoteRuntime.getManifest().countBytes() / 1024L / 1024L)
                                );
                                if(!continueWithoutInstallation) {
                                    throw new MinecraftLauncherAborted("JRE will take up all remaining space");
                                }
                            }
                            downloader.add(jreContainer = javaManager.installVersionNow(remoteRuntime, javaRootDir, forceUpdate));
                            jreExec = remoteRuntime.toLocal(javaRootDir).getExecutableFile().getAbsolutePath();
                        } catch(ExecutionException e) {
                            LOGGER.warn("Couldn't fetch manifest", e);
                            Sentry.capture(new EventBuilder()
                                    .withLevel(Event.Level.WARNING)
                                    .withMessage("couldn't fetch manifest")
                                    .withSentryInterface(new ExceptionInterface(e))
                                    .withExtra("jreName", jreName)
                                    .withExtra("version", versionName)
                            );
                            Optional<JavaRuntimeLocal> localRuntimeOpt = javaManager.getDiscoverer().getCurrentPlatformRuntime(jreName);
                            if(localRuntimeOpt.isPresent()) {
                                LOGGER.info("But local JRE is found. Will use it instead.");
                                if(Alert.showQuestion("", Localizable.get("launcher.warning.jre-manifest-unavailable.use-local"))) {
                                    JavaRuntimeLocal localRuntime = localRuntimeOpt.get();
                                    LOGGER.info("We can continue with the local JRE: {}", localRuntime);
                                    jreExec = localRuntime.getWorkingDirectory().getAbsolutePath();
                                } else {
                                    throw new MinecraftLauncherAborted("Couldn't fetch jre");
                                }
                            } else {
                                LOGGER.info("But local JRE is not found");
                                if(Alert.showQuestion("", Localizable.get("launcher.warning.jre-manifest-unavailable.use-current"))) {
                                    LOGGER.info("We can continue with the current JRE");
                                    jreType = new JavaManagerConfig.Current();
                                } else {
                                    throw new MinecraftLauncherAborted("Couldn't fetch jre");
                                }
                            }
                        } catch(InterruptedException e) {
                            throw new MinecraftLauncherAborted("interrupted while waiting for manifest");
                        }
                    } else {
                        LOGGER.warn("Couldn't find required JRE");
                        if(Alert.showQuestion("", Localizable.get("launcher.warning.jre-not-found"))) {
                            LOGGER.info("User selected to fall back to current JRE");
                            jreType = new JavaManagerConfig.Current();
                        } else {
                            throw new MinecraftLauncherAborted("Couldn't find required JRE");
                        }
                    }
                }
            }
        }

        if(jreType instanceof JavaManagerConfig.Custom) {
            jreExec = ((JavaManagerConfig.Custom) jreType).getPath().orElse(OS.getJavaPath());
        }

        if(jreType instanceof JavaManagerConfig.Current) {
            jreExec = OS.getJavaPath();
        }

        for (MinecraftExtendedListener execContainer1 : extListeners) {
            execContainer1.onMinecraftDownloading();
        }

        DownloadableContainer versionContainer;
        try {
            versionContainer = vm.downloadVersion(versionSync, librariesForType, forceUpdate);
        } catch (IOException var8) {
            throw new MinecraftException(false, "Cannot download version!", "download-jar", var8);
        }

        checkAborted();

        if (assets1 != null) {
            DownloadableContainer assetsContainer = am.downloadResources(version, assets1);
            downloader.add(assetsContainer);
        }

        downloader.add(versionContainer);

        for (MinecraftLauncherAssistant e : assistants) {
            e.collectResources(downloader);
        }

        downloader.startDownloadAndWait();
        if (versionContainer.isAborted() || (jreContainer != null && jreContainer.isAborted())) {
            throw new MinecraftLauncher.MinecraftLauncherAborted(new AbortedDownloadException());
        } else if (!versionContainer.getErrors().isEmpty() || (jreContainer != null && !jreContainer.getErrors().isEmpty())) {
            throw new MinecraftException(false, "Cannot download all required files", "download");
        } else {
            deJureVersion.setUpdatedTime(U.getUTC().getTime());
            try {
                vm.getLocalList().saveVersion(deJureVersion);
            } catch (IOException var7) {
                LOGGER.warn("Cannot save version", var7);
            }
            constructProcess();
        }
    }

    private void constructProcess() throws MinecraftException {
        checkStep(MinecraftLauncher.MinecraftLauncherStep.DOWNLOADING, MinecraftLauncher.MinecraftLauncherStep.CONSTRUCTING);

        extListeners.forEach(MinecraftExtendedListener::onMinecraftReconstructingAssets);

        try {
            localAssetsDir = reconstructAssets();
        } catch (IOException var8) {
            throw new MinecraftException(false, "Cannot reconstruct assets!", "reconstruct-assets", var8);
        }

        extListeners.forEach(MinecraftExtendedListener::onMinecraftUnpackingNatives);

        try {
            unpackNatives(forceUpdate);
        } catch (IOException var7) {
            throw new MinecraftException(false, "Cannot unpack natives!", "unpack-natives", var7);
        }

        checkAborted();

        extListeners.forEach(MinecraftExtendedListener::onMinecraftDeletingEntries);

        try {
            deleteEntries();
        } catch (IOException var6) {
            throw new MinecraftException(false, "Cannot delete entries!", "delete-entries", var6);
        }

        try {
            deleteLibraryEntries();
        } catch (Exception var5) {
            throw new MinecraftException(false, "Cannot delete library entries!", "delete-entries", var5);
        }

        checkAborted();
        LOGGER.info("Constructing process...");

        extListeners.forEach(MinecraftExtendedListener::onMinecraftConstructing);

        ArrayList<String> jvmArgs = new ArrayList<>(), programArgs = new ArrayList<>();
        createJvmArgs(jvmArgs);

        if (this.programArgs != null) {
            programArgs.addAll(Arrays.asList(StringUtils.split(this.programArgs, ' ')));
        }

        launcher = new JavaProcessLauncher(charset, Objects.requireNonNull(jreExec, "jreExec"), new String[0]);
        launcher.directory(isLauncher ? rootDir : gameDir);

        try {
            fixResourceFolder();
        } catch (Exception ioE) {
            LOGGER.warn("Cannot check resource folder. This could have been fixed [MCL-3732].", ioE);
        }


        if (!isLauncher) {
            Set<NBTServer> exisingServerList = null, nbtServerList = new LinkedHashSet<>();
            try {
                File file = new File(gameDir, "servers.dat");
                if (file.isFile()) {
                    try {
                        FileUtil.copyFile(file, new File(file.getAbsolutePath() + ".bak"), true);
                    } catch (IOException ioE) {
                        LOGGER.warn("Could not make backup for servers.dat", ioE);
                    }
                    try {
                        exisingServerList = NBTServer.loadSet(file);
                    } catch (Exception e) {
                        LOGGER.warn("Could not read servers.dat." +
                                "We'll have to overwrite it as it can't be read by Minecraft neither", e);
                        exisingServerList = new LinkedHashSet<>();
                    }
                } else {
                    FileUtil.createFile(file);
                    exisingServerList = new LinkedHashSet<>();
                }
                if (server != null) {
                    nbtServerList.add(new NBTServer(server));
                }
                if (outdatedPromotedServers != null) {
                    Iterator<NBTServer> i = exisingServerList.iterator();
                    while (i.hasNext()) {
                        NBTServer existingServer = i.next();
                        for (PromotedServer outdatedServer : outdatedPromotedServers) {
                            if (existingServer.equals(outdatedServer) && existingServer.getName().equals(outdatedServer.getName())) {
                                LOGGER.debug("Removed outdated server: {}", existingServer);
                                i.remove();
                                break;
                            }
                        }
                    }
                }
                if (settings.getBoolean("minecraft.servers.promoted.ingame")) {
                    if (promotedServers != null) {
                        for (final PromotedServer promotedServer : promotedServers) {
                            if (!promotedServer.getFamily().isEmpty() && !promotedServer.getFamily().contains(family)) {
                                continue;
                            }
                            if (promotedServer.equals(server)) {
                                continue;
                            }
                            NBTServer existingServer = null;
                            for (NBTServer nbtServer : exisingServerList) {
                                if (promotedServer.equals(nbtServer)) {
                                    existingServer = nbtServer;
                                    break;
                                }
                            }
                            if (existingServer != null) {
                                nbtServerList.add(existingServer);
                                exisingServerList.remove(existingServer);
                            } else {
                                nbtServerList.add(new NBTServer(promotedServer));
                            }
                        }
                    } else {
                        promotedServerAddStatus = PromotedServerAddStatus.EMPTY;
                    }
                } else {
                    promotedServerAddStatus = PromotedServerAddStatus.DISABLED;
                }

                nbtServerList.addAll(exisingServerList);
                FileUtil.copyFile(file, new File(gameDir, "servers.dat.bak"), true);
                NBTServer.saveSet(nbtServerList, file);
                if (promotedServerAddStatus == PromotedServerAddStatus.NONE) {
                    promotedServerAddStatus = PromotedServerAddStatus.SUCCESS;
                }
            } catch (Exception e) {
                LOGGER.warn("Couldn't reconstruct server list", e);
                promotedServerAddStatus = PromotedServerAddStatus.ERROR;
            }
        }

        if (!isLauncher) {
            try {
                fixForNewerVersions();
            } catch (Exception e) {
                LOGGER.warn("Could not make it compatible with older versions", e);
            }
        }

        /*launcher.addCommand("-Djava.library.path=" + nativeDir.getAbsolutePath());

        if (OS.WINDOWS.isCurrent() && OS.VERSION.startsWith("10.")) {
            launcher.addCommand("-Dos.name=Windows 10");
            launcher.addCommand("-Dos.version=10.0");
        }

        launcher.addCommand("-cp", constructClassPath(version));
        launcher.addCommand("-Dfml.ignoreInvalidMinecraftCertificates=true");
        launcher.addCommand("-Dfml.ignorePatchDiscrepancies=true");
        launcher.addCommand("-Djava.net.useSystemProxies=true");

        if (!OS.WINDOWS.isCurrent() || StringUtils.isAsciiPrintable(nativeDir.getAbsolutePath())) {
            launcher.addCommand("-Dfile.encoding=UTF-8");
        }

        launcher.addCommands(getJVMArguments());
        if (javaArgs != null) {
            launcher.addSplitCommands(javaArgs);
        }

        address = assistants.iterator();

        MinecraftLauncherAssistant assistant2;
        while (address.hasNext()) {
            assistant2 = (MinecraftLauncherAssistant) address.next();
            assistant2.constructJavaArguments();
        }


        if (!fullCommand) {
            log("Half command (characters are not escaped):\n" + launcher.getCommandsAsString());
        }

        launcher.addCommands(getMinecraftArguments());
        launcher.addCommand("--width", Integer.valueOf(windowSize[0]));
        launcher.addCommand("--height", Integer.valueOf(windowSize[1]));
        if (fullScreen) {
            launcher.addCommand("--fullscreen");
        }

        try {
            File serversDat = new File(gameDir, "servers.dat");

            if (serversDat.isFile())
                FileUtil.copyFile(serversDat, new File(serversDat.getAbsolutePath() + ".bak"), true);

        } catch (IOException ioE) {
            log("Could not make backup for servers.dat", ioE);
        }

        try {
            fixResourceFolder();
        } catch (Exception ioE) {
            log("Cannot check resource folder. This could have been fixed [MCL-3732].", ioE);
        }


        Set<NBTServer> exisingServerList = null, nbtServerList = new LinkedHashSet<>();
        try {
            File file = new File(gameDir, "servers.dat");
            if(file.isFile()) {
                exisingServerList = NBTServer.loadSet(file);
            } else {
                FileUtil.createFile(file);
                exisingServerList = new LinkedHashSet<>();
            }
            if(server != null) {
                nbtServerList.add(new NBTServer(server));
            }
            if (outdatedPromotedServers != null) {
                Iterator<NBTServer> i = exisingServerList.iterator();
                while (i.hasNext()) {
                    NBTServer existingServer = i.next();
                    for(PromotedServer outdatedServer : outdatedPromotedServers) {
                        if(existingServer.equals(outdatedServer) && existingServer.getName().equals(outdatedServer.getName())) {
                            log("Removed outdated server:", existingServer, ", compared with", outdatedServer);
                            i.remove();
                            break;
                        }
                    }
                }
            }
            if(settings.getBoolean("minecraft.servers.promoted.ingame")) {
                if (promotedServers != null) {
                    for (final PromotedServer promotedServer : promotedServers) {
                        if (!promotedServer.getFamily().isEmpty() && !promotedServer.getFamily().contains(family)) {
                            continue;
                        }
                        if(promotedServer.equals(server)) {
                            continue;
                        }
                        NBTServer existingServer = null;
                        for (NBTServer nbtServer : exisingServerList) {
                            if (promotedServer.equals(nbtServer)) {
                                existingServer = nbtServer;
                                break;
                            }
                        }
                        if (existingServer != null) {
                            nbtServerList.add(existingServer);
                            exisingServerList.remove(existingServer);
                        } else {
                            nbtServerList.add(new NBTServer(promotedServer));
                        }
                    }
                } else {
                    promotedServerAddStatus = PromotedServerAddStatus.EMPTY;
                }
            } else {
                promotedServerAddStatus = PromotedServerAddStatus.DISABLED;
            }

            nbtServerList.addAll(exisingServerList);
            if(!nbtServerList.isEmpty()) {
                FileUtil.copyFile(file, new File(gameDir, "servers.dat.bak"), true);
                NBTServer.saveSet(nbtServerList, file);
                if(promotedServerAddStatus == PromotedServerAddStatus.NONE) {
                    promotedServerAddStatus = PromotedServerAddStatus.SUCCESS;
                }
            }
        } catch (Exception e) {
            Sentry.sendError(MinecraftLauncher.class, "couldn't reconstruct server list", e, DataBuilder.create("existing", exisingServerList).add("new", nbtServerList).add("status", promotedServerAddStatus));
            log("Couldn't reconstruct server list", e);
            promotedServerAddStatus = PromotedServerAddStatus.ERROR;
        }

        if (server != null) {
            launcher.addCommand("--server", server.getAddress());
            launcher.addCommand("--port", server.getPort());
        }

        if (programArgs != null) {
            launcher.addSplitCommands(programArgs);
        }

        address = assistants.iterator();

        while (address.hasNext()) {
            assistant2 = (MinecraftLauncherAssistant) address.next();
            assistant2.constructProgramArguments();
        }*/

        StrSubstitutor argumentsSubstitutor = createArgumentsSubstitutor();
        jvmArgs.addAll(version.addArguments(ArgumentType.JVM, featureMatcher, argumentsSubstitutor));
        programArgs.addAll(version.addArguments(ArgumentType.GAME, featureMatcher, argumentsSubstitutor));

        if (!isLauncher && server != null) {
            programArgs.addAll(Arrays.asList("--server", server.getAddress()));
            if(server.getPort() != Server.DEFAULT_PORT) {
                programArgs.addAll(Arrays.asList("--port", String.valueOf(server.getPort())));
            }
        }

        // add modpack-related arguments
        programArgs.addAll(processModpack());

        for (String arg : jvmArgs) {
            launcher.addCommand(arg);
        }

        launcher.addCommand(version.getMainClass());

        if (!fullCommand) {
            List<String> l = new ArrayList<>(launcher.getCommands());
            l.addAll(programArgs);
            LOGGER.info("Half command (not escaped):");
            LOGGER.info(launcher.getJvmPath() + " " + joinList(l, ARGS_CENSORED, BLACKLIST_MODE_CENSOR));
        }

        for (String arg : programArgs) {
            launcher.addCommand(arg);
        }

        if (settings.getBoolean("minecraft.deleteTlSkinCape")) {
            LOGGER.info("Deleting TLSkinCape mod. Disable this feature in config file if you want");
            deleteMod("tl.?skin.?cape.*\\.jar");
        }

        if (fullCommand) {
            LOGGER.info("Full command (not escaped):");
            LOGGER.info(launcher.getCommandsAsString());
        }

        /*    CompatibilityRule.FeatureMatcher featureMatcher = createFeatureMatcher();
    StrSubstitutor argumentsSubstitutor = createArgumentsSubstitutor(getVersion(), this.selectedProfile, gameDirectory, assetsDir, this.auth);

    getVersion().addArguments(net.minecraft.launcher.updater.ArgumentType.JVM, featureMatcher, processBuilder, argumentsSubstitutor);
    processBuilder.withArguments(new String[] { getVersion().getMainClass() });

    LOGGER.info("Half command: " + org.apache.commons.lang3.StringUtils.join(processBuilder.getFullCommands(), " "));

    getVersion().addArguments(net.minecraft.launcher.updater.ArgumentType.GAME, featureMatcher, processBuilder, argumentsSubstitutor);

    Proxy proxy = getLauncher().getProxy();
    PasswordAuthentication proxyAuth = getLauncher().getProxyAuth();
    if (!proxy.equals(Proxy.NO_PROXY)) {
      InetSocketAddress address = (InetSocketAddress)proxy.address();
      processBuilder.withArguments(new String[] { "--proxyHost", address.getHostName() });
      processBuilder.withArguments(new String[] { "--proxyPort", Integer.toString(address.getPort()) });
      if (proxyAuth != null) {
        processBuilder.withArguments(new String[] { "--proxyUser", proxyAuth.getUserName() });
        processBuilder.withArguments(new String[] { "--proxyPass", new String(proxyAuth.getPassword()) });
      }
    }

    processBuilder.withArguments(this.additionalLaunchArgs);
    try
    {
      LOGGER.debug("Running " + org.apache.commons.lang3.StringUtils.join(processBuilder.getFullCommands(), " "));
      GameProcess process = this.processFactory.startGame(processBuilder);
      process.setExitRunnable(this);

      setStatus(GameInstanceStatus.PLAYING);
      if (this.visibilityRule != LauncherVisibilityRule.DO_NOTHING) {
        this.minecraftLauncher.getUserInterface().setVisible(false);
      }
    } catch (IOException e) {
      LOGGER.error("Couldn't launch game", e);
      setStatus(GameInstanceStatus.IDLE);
      return;
    }
*/

        launchMinecraft();
    }

    private void deleteMod(String... names) {
        File[] files = new File(gameDir, "mods").listFiles(file ->
                file.isFile()
                        && Arrays.stream(names).anyMatch(
                                file.getName().toLowerCase(Locale.ROOT)::matches
                        )
        );

        if (files == null) return;
        for (File file : files) {
            LOGGER.debug("Removing {}", file.getName());
            if (!file.delete()) {
                LOGGER.error("error removing mod {}", file.getName());
            }
        }
    }

    private File reconstructAssets() throws IOException, MinecraftException {
        String assetVersion = version.getAssetIndex().getId();
        if(assetVersion == null) {
            LOGGER.warn("Asset version is unknown");
            assetVersion = "unknown";
        }
        File indexFile = new File(assetsIndexesDir, assetVersion + ".json");
        File virtualRoot = new File(new File(globalAssetsDir, "virtual"), assetVersion);
        if (!indexFile.isFile()) {
            LOGGER.warn("No assets index file {}; can't reconstruct assets", virtualRoot);
        } else {
            AssetIndex index;
            try {
                index = U.requireNotNull(gson.fromJson(new FileReader(indexFile), AssetIndex.class), "json response");
            } catch (Exception var9) {
                LOGGER.warn("Couldn't read index file", var9);
                return virtualRoot;
            }

            if (index.isMapToResources()) {
                virtualRoot = new File(gameDir, "resources");
            }

            if (index.isVirtual() || index.isMapToResources()) {
                LOGGER.info("Reconstructing virtual assets folder at {}", virtualRoot);

                for (Entry<String, AssetIndex.AssetObject> stringAssetObjectEntry : index.getFileMap().entrySet()) {
                    checkAborted();

                    File target = new File(virtualRoot, stringAssetObjectEntry.getKey());
                    File original = new File(
                            new File(assetsObjectsDir, stringAssetObjectEntry.getValue().getHash().substring(0, 2)),
                            stringAssetObjectEntry.getValue().getHash()
                    );
                    if (!original.isFile()) {
                        LOGGER.warn("Skipped reconstructing: {}", original);
                    } else if (forceUpdate || !target.isFile()) {
                        FileUtils.copyFile(original, target, false);
                        LOGGER.debug("{} -> {}", original, target);
                    }
                }

                    FileUtil.writeFile(new File(virtualRoot, ".lastused"), dateAdapter.format(new Date()));
            }

        }
        return virtualRoot;
    }

    private void unpackNatives(boolean force) throws IOException {
        LOGGER.info("Unpacking natives...");
        Collection<Library> libraries = version.getRelevantLibraries(featureMatcher);
        OS os = OS.CURRENT;
        ZipFile zip = null;
        if (force) {
            nativeDir.delete();
        }

        Iterator<Library> var7 = libraries.iterator();

        label79:
        while (true) {
            Library library;
            Map<OS, String> nativesPerOs;
            do {
                do {
                    if (!var7.hasNext()) {
                        return;
                    }

                    library = var7.next();
                    nativesPerOs = library.getNatives();
                } while (nativesPerOs == null);
            } while (nativesPerOs.get(os) == null);

            File file = new File(MinecraftUtil.getWorkingDirectory(), "libraries/" + library.getArtifactPath((String) nativesPerOs.get(os)));
            if (!file.isFile()) {
                throw new IOException("Required archive doesn't exist: " + file.getAbsolutePath());
            }

            try {
                zip = new ZipFile(file);
            } catch (IOException var18) {
                throw new IOException("Error opening ZIP archive: " + file.getAbsolutePath(), var18);
            }

            ExtractRules extractRules = library.getExtractRules();
            Enumeration entries = zip.entries();

            while (true) {
                ZipEntry entry;
                File targetFile;
                do {
                    do {
                        do {
                            if (!entries.hasMoreElements()) {
                                zip.close();
                                continue label79;
                            }

                            entry = (ZipEntry) entries.nextElement();
                        } while (extractRules != null && !extractRules.shouldExtract(entry.getName()));

                        targetFile = new File(nativeDir, entry.getName());
                    } while (!force && targetFile.isFile());

                    if (targetFile.getParentFile() != null) {
                        targetFile.getParentFile().mkdirs();
                    }
                } while (entry.isDirectory());

                BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
                byte[] buffer = new byte[2048];
                FileOutputStream outputStream = new FileOutputStream(targetFile);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                int length;
                while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    bufferedOutputStream.write(buffer, 0, length);
                }

                inputStream.close();
                bufferedOutputStream.close();
            }
        }
    }

    private void deleteEntries() throws IOException {
        List<String> entries = version.getDeleteEntries();
        if (entries != null && entries.size() != 0) {
            LOGGER.info("Removing entries...");
            File file = version.getFile(rootDir);
            removeFrom(file, entries);
        }
    }

    private void deleteLibraryEntries() throws IOException {

        for (Library lib : version.getLibraries()) {
            List<String> entries = lib.getDeleteEntriesList();
            if (entries != null && !entries.isEmpty()) {
                LOGGER.debug("Processing entries of {}", lib.getName());
                removeFrom(new File(rootDir, "libraries/" + lib.getArtifactPath()), entries);
            }
        }

    }

    private String constructClassPath(CompleteVersion version) throws MinecraftException {
        LOGGER.info("Constructing classpath...");
        StringBuilder result = new StringBuilder();
        Collection<File> classPath = version.getClassPath(OS.CURRENT, featureMatcher, rootDir);
        String separator = System.getProperty("path.separator");

        File file;
        for (Iterator<File> var6 = classPath.iterator(); var6.hasNext(); result.append(file.getAbsolutePath())) {
            file = var6.next();
            if (!file.isFile()) {
                throw new MinecraftException(true, "Classpath is not found: " + file, "classpath", file);
            }

            if (result.length() > 0) {
                result.append(separator);
            }
        }

        return result.toString();
    }

    private void fixForNewerVersions() throws MinecraftException {
        boolean needSave = false;
        if (version.getMinecraftArguments() != null && version.hasModernArguments()) {
            deJureVersion.setMinecraftArguments(null);
            needSave = true;
        }
        if (needSave) {
            try {
                vm.getLocalList().saveVersion(deJureVersion);
            } catch (IOException var7) {
                LOGGER.warn("Cannot save legacy arguments!", var7);
            }
        }
    }

    private String makeLegacyArgumentString(ArgumentType type) {
        List<String> argList = version.addArguments(type, featureMatcher, null);
        return joinList(argList, ARGS_LEGACY_REMOVED, BLACKLIST_MODE_REMOVE);
    }

    private void removeOldModlistFiles() {
        File[] fileList = gameDir.listFiles();
        if (fileList == null) {
            LOGGER.warn("Cannot get file list in {}", rootDir);
            return;
        }
        for (File file : fileList) {
            if (file.getName().startsWith("tempModList-")) {
                FileUtil.deleteFile(file);
            }
        }
    }

    private List<String> processModpack() {
        removeOldModlistFiles();

        List<Library> mods = version.getMods(featureMatcher);

        if (mods.size() == 0) return Collections.emptyList();

        ModpackType modpackType = version.getModpackType();

        switch (modpackType) {
            case FORGE_LEGACY:
            case FORGE_LEGACY_ABSOLUTE:
                ModList modList = new ModList(
                        new File(rootDir, "libraries"),
                        modpackType == ModpackType.FORGE_LEGACY_ABSOLUTE);
                mods.forEach(modList::addMod);
                String modListFilename = "tempModList-" + System.currentTimeMillis();
                try {
                    modList.save(new File(gameDir, modListFilename));
                } catch (IOException e) {
                    LOGGER.warn("Cannot generate mod list file", e);
                    return Collections.emptyList();
                }

                return Arrays.asList("--modListFile", modListFilename);

            case FORGE_1_13:
                return Arrays.asList(
                        "--fml.mods",
                        mods.stream().map(Library::getName).collect(Collectors.joining(",")),
                        "--fml.mavenRoots",
                        settings.getBoolean("minecraft.gamedir.separate") ? "../../libraries" : "libraries"
                );

            default:
                return Collections.emptyList();
        }
    }

    private static final String[] ARGS_LEGACY_REMOVED = new String[]{
            "--width", "${resolution_width}", "--height", "${resolution_height}"
    };

    private static final String[] ARGS_CENSORED = new String[]{
            "--accessToken"
    };

    private static final String[] CENSORED = new String[]{
            "not for you", "censored", "nothinginteresting", "boiiiiiiiiii",
            "Minecraft is a lie", "vk.cc/7iPiB9", "worp-worp"
    };

    private static final int BLACKLIST_MODE_REMOVE = 0, BLACKLIST_MODE_CENSOR = 1;

    private String joinList(List<String> l, String[] blackList, int blacklistMode) {
        StringBuilder b = new StringBuilder();
        Iterator<String> i = l.iterator();
        while (i.hasNext()) {
            String arg = i.next();
            if (U.find(arg, blackList) == -1) {
                b.append(' ').append(arg);
            } else {
                if (blacklistMode == BLACKLIST_MODE_CENSOR) {
                    b.append(' ').append(arg).append(" [").append(U.getRandom(CENSORED)).append("]");
                    if (i.hasNext()) {
                        i.next(); // skip
                    }
                }
            }
        }
        if (b.length() > 1) {
            return b.substring(1);
        }
        return null;
    }

    /*private String[] getMinecraftArguments() throws MinecraftException {
        log("Getting Minecraft arguments...");
        if (version.getMinecraftArguments() == null) {
            throw new MinecraftException(true, "Can\'t run version, missing minecraftArguments", "noArgs");
        } else {
            HashMap map = new HashMap();
            StrSubstitutor substitutor = new StrSubstitutor(map);
            String assets = version.getAssetIndex().getId();
            String[] split = version.getMinecraftArguments().split(" ");
            map.putAll(account.getUser().getLoginCredentials().map());
            /*map.put("auth_username", accountName);
            if (!account.isFree()) {
                map.put("auth_session", String.format(java.util.Locale.ROOT, "token:%s:%s", account.getAccessToken(), account.getProfile().getId()));
                map.put("auth_access_token", account.getAccessToken());
                map.put("user_properties", gson.toJson(account.getProperties()));
                map.put("auth_player_name", account.getDisplayName());
                map.put("auth_uuid", account.getUUID());
                map.put("user_type", "mojang");
                map.put("profile_name", account.getProfile().getName());
            } else {
                map.put("auth_session", "null");
                map.put("auth_access_token", "null");
                map.put("user_properties", "[]");
                map.put("auth_player_name", accountName);
                map.put("auth_uuid", (new UUID(0L, 0L)).toString());
                map.put("user_type", "legacy");
                map.put("profile_name", "(Default)");
            }**

            map.put("version_name", version.getID());
            map.put("version_type", version.getReleaseType());
            map.put("game_directory", gameDir.getAbsolutePath());
            map.put("game_assets", localAssetsDir.getAbsolutePath());
            map.put("assets_root", globalAssetsDir.getAbsolutePath());
            map.put("assets_index_name", assets == null ? "legacy" : assets);

            for (int i = 0; i < split.length; ++i) {
                split[i] = substitutor.replace(split[i]);
            }

            return split;
        }
    }*/

    private void createJvmArgs(List<String> args) {
        javaManagerConfig.getArgs().ifPresent(s ->
                args.addAll(Arrays.asList(StringUtils.split(s, ' ')))
        );
        if (javaManagerConfig.useOptimizedArguments()) {
            if (getJreMajorVersion() == 0 || getJreMajorVersion() >= 9 || ramSize >= 3072) {
                args.add("-XX:+UnlockExperimentalVMOptions"); // to unlock G1NewSizePercent
                args.add("-XX:+UseG1GC");
                args.add("-XX:G1NewSizePercent=20"); // from Mojang launcher
                args.add("-XX:G1ReservePercent=20"); // from Mojang launcher
                args.add("-XX:MaxGCPauseMillis=50"); // from Mojang launcher
                args.add("-XX:G1HeapRegionSize=32M"); // from Mojang launcher
                args.add("-XX:ConcGCThreads=" + Math.max(1, OS.Arch.AVAILABLE_PROCESSORS / 4));
                args.add("-XX:ParallelGCThreads=" + OS.Arch.AVAILABLE_PROCESSORS);
            } else {
                args.add("-XX:+UseConcMarkSweepGC");
                args.add("-XX:-UseAdaptiveSizePolicy");
                args.add("-XX:+CMSParallelRemarkEnabled");
                args.add("-XX:+ParallelRefProcEnabled");
                args.add("-XX:+CMSClassUnloadingEnabled");
                args.add("-XX:+UseCMSInitiatingOccupancyOnly");
            }
        }
        args.add("-Xmx" + ramSize + "M");
        if (librariesForType == Account.AccountType.MCLEAKS) {
            args.add("-Dru.turikhay.mcleaks.nstweaker.hostname=true");
            args.add("-Dru.turikhay.mcleaks.nstweaker.hostname.list=" + NSTweaker.toTweakHostnameList(McleaksManager.getConnector().getList()));
            args.add("-Dru.turikhay.mcleaks.nstweaker.ssl=true");
            args.add("-Dru.turikhay.mcleaks.nstweaker.mainclass=" + oldMainclass);
        }
        if (!OS.WINDOWS.isCurrent() || StringUtils.isAsciiPrintable(nativeDir.getAbsolutePath())) {
            args.add("-Dfile.encoding=" + charset.name());
        }
    }

    private AssetsManager.ResourceChecker resourceChecker;

    private List<AssetIndex.AssetObject> compareAssets(boolean fastCompare) throws MinecraftException {
        if (version.getAssetIndex() != null && "none".equals(version.getAssetIndex().getId())) {
            LOGGER.info("Assets comparison skipped");
            return null;
        }

        LOGGER.info("Checking assets...");

        AssetsManager.ResourceChecker checker;
        try {
            checker = am.checkResources(version, fastCompare);
        } catch (AssetsNotFoundException e) {
            LOGGER.warn("Couldn't check resources", e);
            return null;
        }

        try {
            resourceChecker = checker;
            boolean showTimerWarning = true;
            AssetIndex.AssetObject lastObject = null;
            int timer = 0;

            while (working && checker.checkWorking()) {
                final AssetIndex.AssetObject object = checker.getCurrent();
                if (object != null) {
                    LOGGER.debug("Instant state on: {}", object);
                    if (showTimerWarning && object == lastObject) {
                        if (++timer == 10) {
                            LOGGER.warn("We're checking this object for too long: {}", object);
                            AsyncThread.execute(new Runnable() {
                                @Override
                                public void run() {
                                    Alert.showLocWarning("launcher.warning.assets.long");
                                }
                            });
                            showTimerWarning = false;
                        }
                    } else {
                        timer = 0;
                    }
                    U.sleepFor(1000);
                }
                lastObject = object;
            }
        } catch (InterruptedException inE) {
            throw new MinecraftLauncherAborted(inE);
        }

        checkAborted();

        List<AssetIndex.AssetObject> result = checker.getAssetList();
        if (result == null) {
            LOGGER.error("Could not check assets", checker.getError());
            return Collections.emptyList();
        }

        LOGGER.info("Compared assets in {} ms", checker.getDelta());
        return result;
    }

    private void fixResourceFolder() throws Exception {
        if (isLauncher) {
            return;
        }
        File serverResourcePacksFolder = new File(gameDir, "server-resource-packs");
        if (serverResourcePacksFolder.isDirectory()) {
            File[] files = U.requireNotNull(serverResourcePacksFolder.listFiles(), "files of " + serverResourcePacksFolder.getAbsolutePath());
            for (File file : files) {
                if (file.length() == 0) {
                    FileUtil.deleteFile(file);
                }
            }
        }
        FileUtil.createFolder(serverResourcePacksFolder);
    }

    private void launchMinecraft() throws MinecraftException {
        checkStep(MinecraftLauncher.MinecraftLauncherStep.CONSTRUCTING, MinecraftLauncher.MinecraftLauncherStep.LAUNCHING);
        Iterator var2 = listeners.iterator();

        while (var2.hasNext()) {
            MinecraftListener e = (MinecraftListener) var2.next();
            e.onMinecraftLaunch();
        }

        try {
            processLogger = ChildProcessLogger.create(charset);
        } catch (IOException e) {
            LOGGER.warn("Cannot create process logger", e);
        }

        if (version.getReleaseType() != null)
            switch (version.getReleaseType()) {
                case RELEASE:
                case SNAPSHOT:
                    LOGGER.info("Starting Minecraft {}", version.getID());
                    break;
                default:
                    LOGGER.info("Starting {}", version.getID());
            }
        LOGGER.debug("Launching in: {}", gameDir);
        startupTime = System.currentTimeMillis();


        try {
            ProcessBuilder b = launcher.createProcess();
            Map<String, String> env = b.environment();
            if(env != null) {
                env.put("_JAVA_OPTIONS", "");
            }
            process = new JavaProcess(b.start(), charset);
            process.safeSetExitRunnable(this);
            minecraftWorking = true;
            updateLoggerActions();
        } catch (Exception var3) {
            notifyClose();
            if (var3.getMessage().contains("CreateProcess error=2,")) {
                throw new MinecraftException(false, "Executable is not found: \"" + var3.getMessage() + "\"", "exec-not-found");
            }
            throw new MinecraftException(true, "Cannot start the game!", "start", var3);
        }

        postLaunch();
    }

    private static void updateLoggerActions() {
        TLauncher.getInstance().updateLoggerUIActions();
    }

    private void postLaunch() {
        checkStep(MinecraftLauncher.MinecraftLauncherStep.LAUNCHING, MinecraftLauncher.MinecraftLauncherStep.POSTLAUNCH);
        LOGGER.info("Post-launch actions are proceeding");

        Iterator var2 = extListeners.iterator();
        while (var2.hasNext()) {
            MinecraftExtendedListener listener = (MinecraftExtendedListener) var2.next();
            listener.onMinecraftPostLaunch();
        }

        Stats.minecraftLaunched(account, version, server, serverId, promotedServerAddStatus);
        if (assistLaunch) {
            LOGGER.info("Waiting child process to close");
            waitForClose();
        } else {
            LOGGER.info("Going to close in 30 seconds");
            U.sleepFor(30000L);
            if (minecraftWorking) {
                TLauncher.kill();
            }
        }

    }

    public void killProcess() {
        if (!minecraftWorking) {
            throw new IllegalStateException();
        } else {
            LOGGER.info("Killing child process forcefully");
            killed = true;
            updateLoggerActions();
            process.stop();
        }
    }

    private void checkThread() {
        if (!Thread.currentThread().equals(parentThread)) {
            throw new IllegalStateException("Illegal thread!");
        }
    }

    private void checkStep(MinecraftLauncher.MinecraftLauncherStep prevStep, MinecraftLauncher.MinecraftLauncherStep currentStep) {
        checkAborted();
        if (prevStep != null && currentStep != null) {
            if (!step.equals(prevStep)) {
                throw new IllegalStateException("Called from illegal step: " + step);
            } else {
                checkThread();
                step = currentStep;
            }
        } else {
            throw new NullPointerException("NULL: " + prevStep + " " + currentStep);
        }
    }

    private void checkAborted() {
        if (!working) {
            throw new MinecraftLauncher.MinecraftLauncherAborted("Aborted at step: " + step);
        }
    }

    private void checkWorking() {
        if (working) {
            throw new IllegalStateException("Launcher is working!");
        }
    }

    // log4j2 markers
    private static final Marker
            CHILD_STDOUT = MarkerManager.getMarker("child_stdout"),
            CHILD_STDERR = MarkerManager.getMarker("child_stderr");

    // PrintStreamType -> Marker
    private static final Marker[] MARKERS;
    static {
        PrintStreamType[] types = PrintStreamType.values();
        Validate.isTrue(types.length == 2,
                "please check " + PrintStreamType.class.getSimpleName() + "values");
        MARKERS = new Marker[types.length];
        MARKERS[PrintStreamType.OUT.ordinal()] = CHILD_STDOUT;
        MARKERS[PrintStreamType.ERR.ordinal()] = CHILD_STDERR;
    }

    @Override
    public void onJavaProcessPrint(JavaProcess process, PrintStreamType streamType, String line) {
        LOGGER.info(MARKERS[streamType.ordinal()], line);

        /*
            STDERR is unbuffered, so it might interfere with STDOUT lines.
            We don't know (yet) if there's *anything* in STDERR that
            CrashManager can make use of.
         */
        if(processLogger != null && streamType == PrintStreamType.OUT) {
            processLogger.log(line);
        }
    }

    @Override
    public void onJavaProcessEnded(JavaProcess jp) {
        notifyClose();

        int exit = jp.getExitCode();

        LOGGER.info("Child process closed with exit code: {} ({})",exit, "0x" + Integer.toHexString(exit));
        exitCode = exit;

        if(processLogger != null) {
            try {
                processLogger.close();
            } catch (IOException e) {
                LOGGER.warn("Process logger failed to close", e);
            }
        }

        if (settings.getBoolean("minecraft.crash") && !killed && (System.currentTimeMillis() - startupTime < MIN_WORK_TIME || exit != 0)) {
            crashManager = new CrashManager(this);

            for (MinecraftListener listener : listeners) {
                listener.onCrashManagerInit(crashManager);
            }

            crashManager.startAndJoin();

            if (crashManager.getCrash().getEntry() == null || !crashManager.getCrash().getEntry().isFake()) {
                return;
            }
        }

        if (!assistLaunch) {
            TLauncher.kill();
        }
    }

    public void onJavaProcessError(JavaProcess jp, Throwable e) {
        notifyClose();
        Iterator var4 = listeners.iterator();

        while (var4.hasNext()) {
            MinecraftListener listener = (MinecraftListener) var4.next();
            listener.onMinecraftError(e);
        }

    }

    private synchronized void waitForClose() {
        while (minecraftWorking) {
            try {
                wait();
            } catch (InterruptedException var2) {
            }
        }

    }

    private synchronized void notifyClose() {
        minecraftWorking = false;

        updateLoggerActions();

        if (System.currentTimeMillis() - startupTime < 5000L) {
            U.sleepFor(1000L);
        }

        notifyAll();
        Iterator var2 = listeners.iterator();

        while (var2.hasNext()) {
            MinecraftListener listener = (MinecraftListener) var2.next();
            listener.onMinecraftClose();
        }

    }

    private void removeFrom(File zipFile, List<String> entries) throws IOException {
        File tempFile = new File(zipFile.getAbsolutePath() + "." + System.currentTimeMillis());
        tempFile.delete();
        tempFile.deleteOnExit();
        boolean renameOk = zipFile.renameTo(tempFile);
        if (!renameOk) {
            throw new IOException("Could not rename the file " + zipFile.getAbsolutePath() + " -> " + tempFile.getAbsolutePath());
        } else {
            LOGGER.debug("Removing entries from {}", zipFile);
            byte[] buf = new byte[1024];
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempFile)));
            ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

            for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
                String name = entry.getName();
                if (entries.contains(name)) {
                    LOGGER.debug("Removed: {}", name);
                } else {
                    zout.putNextEntry(new ZipEntry(name));

                    int len;
                    while ((len = zin.read(buf)) > 0) {
                        zout.write(buf, 0, len);
                    }
                }
            }

            zin.close();
            zout.close();
            tempFile.delete();
        }
    }

    private Rule.FeatureMatcher createFeatureMatcher() {
        return new CurrentLaunchFeatureMatcher();
    }

    private StrSubstitutor createArgumentsSubstitutor() throws MinecraftException {
        Map<String, String> map = new HashMap<>();


        map.putAll(account.getUser().getLoginCredentials().map());

        /*map.put("auth_access_token", user.getAuthenticatedToken());
        map.put("user_properties", new GsonBuilder().registerTypeAdapter(PropertyMap.class, new com.mojang.launcher.LegacyPropertyMapSerializer()).create().toJson(authentication.getUserProperties()));
        map.put("user_property_map", new GsonBuilder().registerTypeAdapter(PropertyMap.class, new com.mojang.authlib.properties.PropertyMap.Serializer()).create().toJson(authentication.getUserProperties()));

        if ((authentication.isLoggedIn()) && (authentication.canPlayOnline())) {
            if ((authentication instanceof com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication)) {
                map.put("auth_session", String.format(java.util.Locale.ROOT, "token:%s:%s", new Object[] { authentication.getAuthenticatedToken(), UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId()) }));
            } else {
                map.put("auth_session", authentication.getAuthenticatedToken());
            }
        }
        else {
            map.put("auth_session", "-");
        }

        if (authentication.getSelectedProfile() != null) {
            map.put("auth_player_name", authentication.getSelectedProfile().getName());
            map.put("auth_uuid", UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId()));
            map.put("user_type", authentication.getUserType().getName());
        } else {
            map.put("auth_player_name", "Player");
            map.put("auth_uuid", new UUID(0L, 0L).toString());
            map.put("user_type", UserType.LEGACY.getName());
        }

        map.put("profile_name", selectedProfile.getName());*/

        map.put("version_name", version.getID());

        map.put("game_directory", gameDir.getAbsolutePath());
        map.put("game_assets", localAssetsDir.getAbsolutePath());

        map.put("assets_root", globalAssetsDir.getAbsolutePath());
        map.put("assets_index_name", version.getAssetIndex().getId());

        map.put("version_type", version.getType());

        String libraryDirPath = new File(this.rootDir, "libraries").getAbsolutePath();
        map.put("library_directory", libraryDirPath);

        map.put("game_libraries_directory", libraryDirPath);
        map.put("forge_transformers",
                version.getTransformers(featureMatcher)
                        .stream()
                        .map(Library::getName)
                        .collect(Collectors.joining(","))
        );

        if (windowSize[0] > 0 && windowSize[1] > 0) {
            map.put("resolution_width", String.valueOf(windowSize[0]));
            map.put("resolution_height", String.valueOf(windowSize[1]));
        } else {
            map.put("resolution_width", "");
            map.put("resolution_height", "");
        }

        map.put("language", "en-us");

        if (resourceChecker != null) {
            for (AssetIndex.AssetObject asset : resourceChecker.getAssetList()) {
                String hash = asset.getHash();
                String path = new File(assetsObjectsDir, hash.substring(0, 2) + "/" + hash).getAbsolutePath();
                map.put("asset=" + asset.getHash(), path);
            }
        }

        map.put("launcher_name", "java-minecraft-launcher");
        map.put("launcher_version", CAPABLE_WITH);
        map.put("natives_directory", this.nativeDir.getAbsolutePath());
        map.put("classpath", constructClassPath(version));
        map.put("classpath_separator", System.getProperty("path.separator"));
        map.put("primary_jar", new File(rootDir, "versions/" + version.getID() + "/" + version.getID() + ".jar").getAbsolutePath());

        return new StrSubstitutor(map);
    }

    private JavaVersion javaVersion;

    private int getJreMajorVersion() throws MinecraftLauncherAborted {
        if(javaVersion == null) {
            if (jreType instanceof JavaManagerConfig.Current) {
                javaVersion = OS.JAVA_VERSION;
            } else {
                JavaVersionDetector detector = new JavaVersionDetector(Objects.requireNonNull(
                        jreExec, "jreExec"));
                try {
                    javaVersion = detector.detect();
                } catch (JavaVersionNotDetectedException e) {
                    LOGGER.warn("Couldn't detect Java version", e);
                    javaVersion = JavaVersion.UNKNOWN;
                } catch (InterruptedException interruptedException) {
                    throw new MinecraftLauncherAborted(interruptedException);
                }
            }
        }
        return javaVersion.getMajor();
    }

    public enum LoggerVisibility {
        ALWAYS,
        ON_CRASH,
        NONE
    }

    class MinecraftLauncherAborted extends RuntimeException {
        MinecraftLauncherAborted(String message) {
            super(message);
        }

        MinecraftLauncherAborted(Throwable cause) {
            super(cause);
        }
    }

    public enum MinecraftLauncherStep {
        NONE,
        COLLECTING,
        DOWNLOADING,
        CONSTRUCTING,
        LAUNCHING,
        POSTLAUNCH
    }
}
