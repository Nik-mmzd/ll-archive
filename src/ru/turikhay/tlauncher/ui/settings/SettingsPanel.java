package ru.turikhay.tlauncher.ui.settings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.configuration.Configuration.ActionOnLaunch;
import ru.turikhay.tlauncher.configuration.Configuration.ConnectionQuality;
import ru.turikhay.tlauncher.configuration.Configuration.ConsoleType;
import ru.turikhay.tlauncher.managers.VersionLists;
import ru.turikhay.tlauncher.ui.alert.Alert;
import ru.turikhay.tlauncher.ui.block.Blocker;
import ru.turikhay.tlauncher.ui.converter.ActionOnLaunchConverter;
import ru.turikhay.tlauncher.ui.converter.ConnectionQualityConverter;
import ru.turikhay.tlauncher.ui.converter.ConsoleTypeConverter;
import ru.turikhay.tlauncher.ui.converter.LocaleConverter;
import ru.turikhay.tlauncher.ui.editor.EditorCheckBox;
import ru.turikhay.tlauncher.ui.editor.EditorComboBox;
import ru.turikhay.tlauncher.ui.editor.EditorFieldChangeListener;
import ru.turikhay.tlauncher.ui.editor.EditorFieldHandler;
import ru.turikhay.tlauncher.ui.editor.EditorFileField;
import ru.turikhay.tlauncher.ui.editor.EditorGroupHandler;
import ru.turikhay.tlauncher.ui.editor.EditorHandler;
import ru.turikhay.tlauncher.ui.editor.EditorPair;
import ru.turikhay.tlauncher.ui.editor.EditorPanel;
import ru.turikhay.tlauncher.ui.editor.EditorResolutionField;
import ru.turikhay.tlauncher.ui.editor.EditorTextField;
import ru.turikhay.tlauncher.ui.explorer.FileExplorer;
import ru.turikhay.tlauncher.ui.explorer.ImageFileExplorer;
import ru.turikhay.tlauncher.ui.loc.LocalizableButton;
import ru.turikhay.tlauncher.ui.loc.LocalizableMenuItem;
import ru.turikhay.tlauncher.ui.login.LoginException;
import ru.turikhay.tlauncher.ui.login.LoginListener;
import ru.turikhay.tlauncher.ui.scenes.DefaultScene;
import ru.turikhay.tlauncher.ui.swing.ImageButton;
import ru.turikhay.tlauncher.ui.swing.extended.BorderPanel;
import ru.turikhay.util.OS;
import net.minecraft.launcher.versions.ReleaseType;

public class SettingsPanel extends EditorPanel implements LoginListener {
	private static final long serialVersionUID = 3896900830909661270L;

	private final DefaultScene scene;

	private final EditorFieldHandler directory;
	private final EditorFieldHandler resolution;
	private final EditorFieldHandler javaPath;
	private final EditorFieldHandler javaArgs;
	private final EditorFieldHandler args;
	private final EditorFieldHandler slide;
	private final EditorFieldHandler console;
	private final EditorFieldHandler connection;
	private final EditorFieldHandler action;
	private final EditorFieldHandler lang;

	private final EditorGroupHandler versionHandler;

	private final LocalizableButton saveButton;
	private final LocalizableButton defaultButton;
	private final ImageButton homeButton;

	private final JPopupMenu popup;
	private final LocalizableMenuItem infoItem, defaultItem;
	private EditorHandler selectedHandler;

	public SettingsPanel(DefaultScene sc) {
		this.scene = sc;

		FocusListener warning = new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				setMessage("settings.warning");
			}

			@Override
			public void focusLost(FocusEvent e) {
				setMessage(null);
			}
		}, restart = new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				setMessage("settings.restart");
			}

			@Override
			public void focusLost(FocusEvent e) {
				setMessage(null);
			}
		};

		this.directory = new EditorFieldHandler("minecraft.gamedir",
				new EditorFileField("settings.client.gamedir.prompt",
						new FileExplorer(FileExplorer.DIRECTORIES_ONLY, true)),
				warning);
		directory.addListener(new EditorFieldChangeListener() {
			@Override
			protected void onChange(String oldValue, String newValue) {
				if (!tlauncher.isReady())
					return;

				try {
					tlauncher.getManager().getComponent(VersionLists.class)
							.updateLocal();
				} catch (IOException e) {
					Alert.showLocError("settings.client.gamedir.noaccess", e);
					return;
				}

				tlauncher.getVersionManager().asyncRefresh();
				tlauncher.getProfileManager().recreate();
			}
		});
		add(new EditorPair("settings.client.gamedir.label",
				directory));

		this.resolution = new EditorFieldHandler("minecraft.size",
				new EditorResolutionField("settings.client.resolution.width",
						"settings.client.resolution.height", global), restart);
		add(new EditorPair("settings.client.resolution.label",
				resolution));

		nextPane();

		ReleaseType[] releaseTypes = ReleaseType.getDefinable();
		EditorFieldHandler[] versions = new EditorFieldHandler[releaseTypes.length];

		for (int i = 0; i < releaseTypes.length; i++) {
			ReleaseType releaseType = releaseTypes[i];
			versions[i] = new EditorFieldHandler("minecraft.versions."
					+ releaseType, new EditorCheckBox("settings.versions."
					+ releaseType));
		}

		this.versionHandler = new EditorGroupHandler(versions);
		versionHandler.addListener(new EditorFieldChangeListener() {
			@Override
			protected void onChange(String oldvalue, String newvalue) {
				TLauncher.getInstance().getVersionManager().updateVersionList();
			}
		});

		add(new EditorPair("settings.versions.label", versions));

		nextPane();

		this.javaArgs = new EditorFieldHandler("minecraft.javaargs",
				new EditorTextField("settings.java.args.jvm", true), warning);
		this.args = new EditorFieldHandler("minecraft.args",
				new EditorTextField("settings.java.args.minecraft", true),
				warning);
		add(new EditorPair("settings.java.args.label", javaArgs,
				args));

		final boolean isWindows = OS.WINDOWS.isCurrent();

		this.javaPath = new EditorFieldHandler("minecraft.javadir",
				new EditorFileField("settings.java.path.prompt", true,
						new FileExplorer(isWindows ? FileExplorer.FILES_ONLY
								: FileExplorer.DIRECTORIES_ONLY, true)) {
					private static final long serialVersionUID = -2220392073262107659L;

					@Override
					public boolean isValueValid() {
						if (!isWindows)
							return true;

						String path = getSettingsValue();
						if (path == null)
							return true;

						File javaDir = new File(path);
						if (javaDir.isFile())
							return true;

						Alert.showLocAsyncError("settings.java.path.doesnotexist");
						return false;
					}

				}, warning);
		add(new EditorPair("settings.java.path.label", javaPath));

		nextPane();

		this.slide = new EditorFieldHandler("gui.background",
				new EditorFileField("settings.slide.list.prompt", true,
						new ImageFileExplorer()));
		slide.addListener(new EditorFieldChangeListener() {
			@Override
			protected void onChange(String oldValue, String newValue) {
				if (!tlauncher.isReady())
					return;
				tlauncher.getFrame().mp.background.SLIDE_BACKGROUND.getThread()
						.asyncRefreshSlide();
			}
		});

		add(new EditorPair("settings.slide.list.label", slide));

		nextPane();

		this.console = new EditorFieldHandler("gui.console",
				new EditorComboBox<ConsoleType>(new ConsoleTypeConverter(),
						ConsoleType.values()));
		console.addListener(new EditorFieldChangeListener() {
			@Override
			protected void onChange(String oldvalue, String newvalue) {
				if (newvalue == null)
					return;
				switch (ConsoleType.get(newvalue)) {
				case GLOBAL:
					TLauncher.getConsole().show(false);
					break;
				case MINECRAFT:
				case NONE:
					TLauncher.getConsole().hide();
					break;
				default:
					throw new IllegalArgumentException("Unknown console type!");
				}
			}
		});
		add(new EditorPair("settings.console.label", console));

		this.connection = new EditorFieldHandler("connection",
				new EditorComboBox<ConnectionQuality>(
						new ConnectionQualityConverter(),
						ConnectionQuality.values()));
		connection.addListener(new EditorFieldChangeListener() {
			@Override
			protected void onChange(String oldValue, String newValue) {
				tlauncher.getDownloader().setConfiguration(
						global.getConnectionQuality());
			}
		});
		add(new EditorPair("settings.connection.label",
				connection));

		this.action = new EditorFieldHandler("minecraft.onlaunch",
				new EditorComboBox<ActionOnLaunch>(
						new ActionOnLaunchConverter(), ActionOnLaunch.values()));
		add(new EditorPair("settings.launch-action.label",
				action));

		this.lang = new EditorFieldHandler("locale",
				new EditorComboBox<Locale>(new LocaleConverter(),
						global.getLocales()));
		lang.addListener(new EditorFieldChangeListener() {
			@Override
			protected void onChange(String oldvalue, String newvalue) {
				if (tlauncher.getFrame() != null)
					tlauncher.getFrame().updateLocales();
			}
		});
		add(new EditorPair("settings.lang.label", lang));

		this.saveButton = new LocalizableButton("settings.save");
		saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveValues();
			}

		});

		this.defaultButton = new LocalizableButton("settings.default");
		defaultButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(Alert.showLocQuestion("settings.default.warning"))
					resetValues();
			}
		});
		
		this.homeButton = new ImageButton("home.png");
		homeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateValues();
				scene.setSettings(false);
			}
		});
		Dimension size = homeButton.getPreferredSize();
		if(size != null)
			homeButton.setPreferredSize(new Dimension(size.width*2, size.height));
		
		BorderPanel controlPanel = new BorderPanel();
		controlPanel.setCenter(sepPan(saveButton, defaultButton));
		controlPanel.setEast(uSepPan(homeButton));
		
		container.add(controlPanel);

		this.popup = new JPopupMenu();

		this.infoItem = new LocalizableMenuItem("settings.popup.info");
		infoItem.setEnabled(false);
		popup.add(infoItem);

		this.defaultItem = new LocalizableMenuItem("settings.popup.default");
		defaultItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedHandler == null)
					return;
				
				resetValue(selectedHandler);
			}
		});
		popup.add(defaultItem);

		for (final EditorHandler handler : this.handlers) {
			Component handlerComponent = handler.getComponent();

			handlerComponent.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() != MouseEvent.BUTTON3)
						return;
					callPopup(e, handler);
				}
			});
		}

		this.updateValues();
	}

	void updateValues() {
		boolean globalUnSaveable = !global.isSaveable();
		for (EditorHandler handler : handlers) {
			String path = handler.getPath(), value = global.get(path);

			handler.updateValue(value);

			if (globalUnSaveable || !global.isSaveable(path))
				Blocker.block(handler, "unsaveable");
		}
	}

	boolean saveValues() {
		if (!checkValues())
			return false;

		for (EditorHandler handler : handlers) {
			String path = handler.getPath(), value = handler.getValue();

			global.set(path, value, false);

			handler.onChange(value);
		}

		global.store();

		return true;
	}

	void resetValues() {
		for (EditorHandler handler : handlers)
			resetValue(handler);
	}

	void resetValue(EditorHandler handler) {
		String path = handler.getPath();

		if (!global.isSaveable(path))
			return;

		String value = global.getDefault(path);

		log("Resetting:", handler.getClass().getSimpleName(), path, value);
		
		handler.setValue(value);
			
		log("Reset!");
	}

	boolean canReset(EditorHandler handler) {
		String key = handler.getPath();

		return global.isSaveable(key)
				&& global.getDefault(handler.getPath()) != null;
	}

	void callPopup(MouseEvent e, EditorHandler handler) {
		if (popup.isShowing())
			popup.setVisible(false);
		
		defocus();

		int x = e.getX(), y = e.getY();
		this.selectedHandler = handler;

		this.updateResetMenu();
		this.infoItem.setVariables(handler.getPath());
		this.popup.show((JComponent) e.getSource(), x, y);
	}

	@Override
	public void block(Object reason) {
		Blocker.blockComponents(container, reason);
		this.updateResetMenu();
	}

	@Override
	public void unblock(Object reason) {
		Blocker.unblockComponents(container, reason);
		this.updateResetMenu();
	}
	
	private void updateResetMenu() {
		if(selectedHandler != null)
			defaultItem.setEnabled( !(Blocker.isBlocked(this) ));
	}

	@Override
	public void onLogin() throws LoginException {
		if (checkValues())
			return;

		scene.setSettings(true);
		throw new LoginException("Invalid settings!");
	}

	@Override
	public void onLoginFailed() {
	}

	@Override
	public void onLoginSuccess() {
	}
}
