package ru.turikhay.tlauncher;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import joptsimple.OptionSet;
import ru.turikhay.tlauncher.Bootstrapper.LoadingStep;
import ru.turikhay.tlauncher.configuration.ArgumentParser;
import ru.turikhay.tlauncher.configuration.Configuration;
import ru.turikhay.tlauncher.configuration.Configuration.ConsoleType;
import ru.turikhay.tlauncher.configuration.LangConfiguration;
import ru.turikhay.tlauncher.downloader.Downloader;
import ru.turikhay.tlauncher.handlers.ExceptionHandler;
import ru.turikhay.tlauncher.handlers.SimpleHostnameVerifier;
import ru.turikhay.tlauncher.managers.ComponentManager;
import ru.turikhay.tlauncher.managers.ComponentManagerListenerHelper;
import ru.turikhay.tlauncher.managers.ProfileManager;
import ru.turikhay.tlauncher.managers.ServerList.Server;
import ru.turikhay.tlauncher.managers.VersionManager;
import ru.turikhay.tlauncher.minecraft.launcher.MinecraftLauncher;
import ru.turikhay.tlauncher.minecraft.launcher.MinecraftListener;
import ru.turikhay.tlauncher.ui.TLauncherFrame;
import ru.turikhay.tlauncher.ui.alert.Alert;
import ru.turikhay.tlauncher.ui.console.Console;
import ru.turikhay.tlauncher.ui.console.Console.CloseAction;
import ru.turikhay.tlauncher.ui.listener.MinecraftUIListener;
import ru.turikhay.tlauncher.ui.listener.RequiredUpdateListener;
import ru.turikhay.tlauncher.ui.loc.Localizable;
import ru.turikhay.tlauncher.ui.login.LoginForm;
import ru.turikhay.tlauncher.updater.Updater;
import ru.turikhay.util.FileUtil;
import ru.turikhay.util.MinecraftUtil;
import ru.turikhay.util.OS;
import ru.turikhay.util.SwingUtil;
import ru.turikhay.util.Time;
import ru.turikhay.util.U;
import ru.turikhay.util.stream.MirroredLinkedStringStream;
import ru.turikhay.util.stream.PrintLogger;

import com.google.gson.Gson;

public class TLauncher {
	// Баги? Есть такое.
	private static final double VERSION = 1.481;
	private static final boolean BETA = false;

	private static TLauncher instance;
	private static String[] sargs;
	private static File directory;

	private static PrintLogger print;
	private static Console console;

	private static Gson gson;

	private TLauncherState state;

	private LangConfiguration lang;
	private Configuration settings;
	private Downloader downloader;
	private Updater updater;

	private TLauncherFrame frame;
	private TLauncherLite lite;

	private ComponentManager manager;
	private VersionManager versionManager;
	private ProfileManager profileManager;

	private final OptionSet args;

	private MinecraftLauncher launcher;

	private RequiredUpdateListener updateListener;
	private MinecraftUIListener minecraftListener;

	private boolean ready;

	private TLauncher(TLauncherState state, OptionSet set) throws Exception {
		if (state == null)
			throw new IllegalArgumentException("TLauncherState can't be NULL!");

		U.log("TLauncher is loading in state", state);

		Time.start(this);
		instance = this;
		this.state = state;
		this.args = set;

		gson = new Gson();

		File
		oldConfig = MinecraftUtil.getSystemRelatedFile("tlauncher.cfg"),
		newConfig = MinecraftUtil.getSystemRelatedDirectory(SETTINGS);

		if(!oldConfig.isFile())
			oldConfig = MinecraftUtil.getSystemRelatedFile(".tlauncher/tlauncher.properties");

		if(oldConfig.isFile() && !newConfig.isFile()) {
			boolean copied = true;

			try {
				FileUtil.createFile(newConfig);
				FileUtil.copyFile(oldConfig, newConfig, true);
			}catch(IOException ioE) {
				U.log("Cannot copy old configuration to the new place", oldConfig, newConfig, ioE);
				copied = false;
			}

			if(copied) {
				U.log("Old configuration successfully moved to the new place:", newConfig);
				FileUtil.deleteFile(oldConfig);
			}
		}

		U.setLoadingStep(LoadingStep.LOADING_CONFIGURATION);
		settings = Configuration.createConfiguration(set);
		reloadLocale();

		U.setLoadingStep(LoadingStep.LOADING_CONSOLE);
		console = new Console(settings, print, "TLauncher Dev Console",
				settings.getConsoleType() == ConsoleType.GLOBAL);
		if (state.equals(TLauncherState.MINIMAL))
			console.setCloseAction(CloseAction.KILL);
		Console.updateLocale();

		U.setLoadingStep(LoadingStep.LOADING_MANAGERS);
		manager = new ComponentManager(this);

		versionManager = manager.loadComponent(VersionManager.class);
		profileManager = manager.loadComponent(ProfileManager.class);

		manager.loadComponent(ComponentManagerListenerHelper.class); // TODO invent something better

		init();

		U.log("Started! (" + Time.stop(this) + " ms.)");

		this.ready = true;
		U.setLoadingStep(LoadingStep.SUCCESS);
	}

	private void init() {
		downloader = new Downloader(this);
		minecraftListener = new MinecraftUIListener(this);

		switch (state) {
		case FULL:

			updater = new Updater(this);
			updateListener = new RequiredUpdateListener(updater);

			U.setLoadingStep(LoadingStep.LOADING_WINDOW);
			frame = new TLauncherFrame(this);

			LoginForm lf = frame.mp.defaultScene.loginForm;

			U.setLoadingStep(LoadingStep.REFRESHING_INFO);
			if (lf.autologin.isEnabled()) {
				versionManager.startRefresh(true);
				lf.autologin.setActive(true);
			} else {
				versionManager.asyncRefresh();
				updater.asyncFindUpdate();
			}

			profileManager.refresh();

			break;
		case MINIMAL:
			lite = new TLauncherLite(this);
			break;
		}
	}

	public Downloader getDownloader() {
		return downloader;
	}

	public LangConfiguration getLang() {
		return lang;
	}

	public Configuration getSettings() {
		return settings;
	}

	public Updater getUpdater() {
		return updater;
	}

	public OptionSet getArguments() {
		return args;
	}

	public TLauncherFrame getFrame() {
		return frame;
	}

	public TLauncherLite getLoader() {
		return lite;
	}

	public static Console getConsole() {
		return console;
	}

	public static Gson getGson() {
		return gson;
	}

	public ComponentManager getManager() {
		return manager;
	}

	public VersionManager getVersionManager() {
		return versionManager;
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}

	public MinecraftLauncher getLauncher() {
		return launcher;
	}

	public MinecraftUIListener getMinecraftListener() {
		return minecraftListener;
	}

	public RequiredUpdateListener getUpdateListener() {
		return updateListener;
	}

	public boolean isReady() {
		return ready;
	}

	public void reloadLocale() throws IOException {
		Locale locale = settings.getLocale();
		U.log("Selected locale: " + locale);

		if (lang == null)
			lang = new LangConfiguration(settings.getLocales(), locale);
		else
			lang.setSelected(locale);

		Localizable.setLang(lang);
		Alert.prepareLocal();
	}

	public void launch(MinecraftListener listener, Server server, boolean forceupdate) {
		this.launcher = new MinecraftLauncher(this, forceupdate);

		launcher.addListener(minecraftListener);
		launcher.addListener(listener);
		launcher.addListener(frame.mp.getProgress());

		launcher.setServer(server);

		launcher.start();
	}

	public boolean isLauncherWorking() {
		return launcher != null && launcher.isWorking();
	}

	public static void kill() {
		U.log("Good bye!");
		System.exit(0);
	}

	public void hide() {
		U.log("I'm hiding!");

		if (frame != null)
			frame.setVisible(false);
	}

	public void show() {
		U.log("Here I am!");

		if (frame != null)
			frame.setVisible(true);
	}

	/* ___________________________________ */

	public static void main(String[] args) {
		ExceptionHandler handler = ExceptionHandler.getInstance();

		Thread.setDefaultUncaughtExceptionHandler(handler);
		Thread.currentThread().setUncaughtExceptionHandler(handler);

		HttpsURLConnection.setDefaultHostnameVerifier(SimpleHostnameVerifier.getInstance());

		U.setPrefix("[TLauncher]");

		MirroredLinkedStringStream stream = new MirroredLinkedStringStream();
		stream.setMirror(System.out);

		print = new PrintLogger(stream);
		stream.setLogger(print);
		System.setOut(print);

		U.setLoadingStep(LoadingStep.INITALIZING);
		SwingUtil.initLookAndFeel();

		try {
			launch(args);
		} catch (Throwable e) {
			U.log("Error launching TLauncher:");
			e.printStackTrace(print);

			Alert.showError(e, true);
		}
	}

	private static void launch(String[] args) throws Exception {
		U.log("Hello!");
		U.log("Starting TLauncher", BRAND, VERSION, "by", DEVELOPER);
		U.log("Have question? Find my e-mail in lang files.");
		U.log("Machine info:", OS.getSummary());
		U.log("Startup time:", Calendar.getInstance().getTime());

		U.log("---");

		TLauncher.sargs = args;

		OptionSet set = ArgumentParser.parseArgs(args);
		if (set == null) {
			new TLauncher(TLauncherState.FULL, null);
			return;
		}

		if (set.has("help"))
			ArgumentParser.getParser().printHelpOn(System.out);

		TLauncherState state = TLauncherState.FULL;
		if (set.has("nogui"))
			state = TLauncherState.MINIMAL;

		new TLauncher(state, set);
	}

	public static String[] getArgs() {
		if (sargs == null)
			sargs = new String[0];
		return sargs;
	}

	public static File getDirectory() {
		if (directory == null)
			directory = new File(".");
		return directory;
	}

	public static TLauncher getInstance() {
		return instance;
	}

	public void newInstance() {
		Bootstrapper.main(sargs);
	}

	public enum TLauncherState {
		FULL, MINIMAL
	}

	/* ___________________________________ */

	private final static String SETTINGS = "tlauncher/legacy.properties", BRAND = "Legacy",
			FOLDER = "minecraft", DEVELOPER = "turikhay";
	private final static String[] DEFAULT_UPDATE_REPO = {
		"http://cdn.turikhay.ru/tlauncher/update/ancient.properties",
		"http://u.tlauncher.ru/update/ancient.properties",
		"http://repo.tlauncher.ru/update/ancient.properties",
	};
	private final static String[]
			OFFICIAL_REPO = {"http://s3.amazonaws.com/Minecraft.Download/" },
			EXTRA_REPO = {};
	private final static String[]
			LIBRARY_REPO = { "https://libraries.minecraft.net/" },
			ASSETS_REPO = { "http://resources.download.minecraft.net/" },
			SERVER_LIST = {};

	public static double getVersion() {
		return VERSION;
	}

	public static boolean isBeta() {
		return BETA;
	}

	public static String getBrand() {
		return BRAND;
	}

	public static String getDeveloper() {
		return DEVELOPER;
	}

	public static String getFolder() {
		return FOLDER;
	}

	public static String[] getUpdateRepos() {
		return DEFAULT_UPDATE_REPO;
	}

	public static String getSettingsFile() {
		return SETTINGS;
	}

	public static String[] getOfficialRepo() {
		return OFFICIAL_REPO;
	}

	public static String[] getExtraRepo() {
		return EXTRA_REPO;
	}

	public static String[] getLibraryRepo() {
		return LIBRARY_REPO;
	}

	public static String[] getAssetsRepo() {
		return ASSETS_REPO;
	}

	public static String[] getServerList() {
		return SERVER_LIST;
	}
}