package net.legacylauncher.ui;

import com.formdev.flatlaf.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.legacylauncher.LegacyLauncher;
import net.legacylauncher.portals.Portals;
import net.legacylauncher.util.FlatLafConfiguration;
import net.legacylauncher.util.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static net.legacylauncher.util.FlatLafConfiguration.getVersion;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FlatLaf {
    private static final Logger LOGGER = LogManager.getLogger(FlatLaf.class);
    private static final String SUPPORTED_CONFIG_VERSION = "v1";

    private static final Lazy<Boolean> IS_SUPPORTED = Lazy.of(() -> {
        try {
            if (!LegacyLauncher.getInstance().isMetadataEnabled("has_flatlaf")) {
                throw new RuntimeException("capability missing: has_flatlaf");
            }
            String configVersion = getVersion();
            if(!configVersion.equals(SUPPORTED_CONFIG_VERSION)) {
                throw new RuntimeException("version not supported: " + configVersion);
            }
        } catch(Throwable t) {
            LOGGER.warn("FlatLafConfiguration not available", t);
            return false;
        }
        return true;
    });

    private static final Lazy<List<String>> STATES = Lazy.of(() -> {
        if(!isSupported()) {
            return Collections.emptyList();
        }
        return Arrays.stream(FlatLafConfiguration.State.values())
                .map(FlatLafConfiguration.State::toString)
                .collect(Collectors.toList());
    });

    public static Map<String, String> getDefaults() {
        if(isSupported()) {
            return FlatLafConfiguration.getDefaults();
        } else {
            return Collections.emptyMap();
        }
    }

    public static Optional<FlatLafConfiguration> parseFromMap(Map<String, String> map) {
        if(isSupported()) {
            return Optional.of(FlatLafConfiguration.parseFromMap(map));
        } else {
            return Optional.empty();
        }
    }

    public static BufferedImage loadDefaultBackgroundFromThemeFile(String themeFile) {
        if (!themeFile.startsWith(":")) { // not a selector
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(themeFile), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                JsonObject o = json.getAsJsonObject();
                if (o.has("_tl")) {
                    JsonObject tlSection = o.getAsJsonObject("_tl");
                    if (tlSection.has("defaultBackground")) {
                        String defaultBackgroundPath = tlSection.getAsJsonPrimitive("defaultBackground")
                                .getAsString();
                        return ImageIO.read(new File(defaultBackgroundPath));
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Couldn't read default background from theme file: {}", themeFile, e);
            }
        }
        return null;
    }

    public static Optional<FlatLafConfiguration.Theme> getSelectedNowTheme(Optional<FlatLafConfiguration> configuration) {
        if (isSupported() && configuration.isPresent() && configuration.get().isEnabled()) {
            FlatLafConfiguration flatLafConfiguration = configuration.get();
            return Optional.of(flatLafConfiguration.getSelected().orElse(
                    UIManager.getBoolean("laf.dark") ?
                            FlatLafConfiguration.Theme.DARK : FlatLafConfiguration.Theme.LIGHT)
            );
        }
        return Optional.empty();
    }

    public static boolean isSupported() {
        return IS_SUPPORTED.get();
    }

    public static List<String> getStates() {
        return STATES.get();
    }

    public static void initialize(FlatLafConfiguration config) {
        if (config.isEnabled()) {
            FlatLafConfiguration.Theme theme = config.getSelected().orElse(detectTheme());
            setUIProperties(config.getUiPropertiesFiles().get(theme));
            setLaf(theme, config.getThemeFiles().get(theme));
        } else {
            LOGGER.info("FlatLaf is not enabled. Skipping initialization");
        }
    }

    private static FlatLafConfiguration.Theme detectTheme() {
        LOGGER.info("Detecting system theme");
        switch (Portals.getPortal().getColorScheme()) {
            case PREFER_LIGHT:
            case NO_PREFERENCE:
            default:
                return FlatLafConfiguration.Theme.LIGHT;
            case PREFER_DARK:
                return FlatLafConfiguration.Theme.DARK;
        }
    }

    private static void setLaf(FlatLafConfiguration.Theme theme, String themeFile) {
        com.formdev.flatlaf.FlatLaf laf = null;
        boolean useSystemTheme = false;
        if (themeFile != null) {
            if (themeFile.startsWith(":")) {
                switch (themeFile.substring(1)) {
                    case "darcula":
                        laf = new FlatDarculaLaf();
                        break;
                    case "dark":
                        laf = new FlatDarkLaf();
                        break;
                    case "intellij":
                        laf = new FlatIntelliJLaf();
                        break;
                    case "light":
                        laf = new FlatLightLaf();
                        break;
                    case "system":
                        useSystemTheme = true;
                        break;
                    default:
                        LOGGER.warn("unknown theme id {}", themeFile);
                        laf = new FlatLightLaf();
                        break;
                }
            } else {
                laf = loadLafFromThemeFile(themeFile);
            }
        }
        if (useSystemTheme) {
            LOGGER.info("System L&F is selected for theme {}", theme);
            if (theme == FlatLafConfiguration.Theme.DARK) {
                UIManager.put("laf.dark", true);
            }
            setSystemLookAndFeel();
            return;
        }
        if (laf == null) {
            switch (theme) {
                case DARK:
                    laf = new FlatDarkLaf();
                    break;
                case LIGHT:
                    laf = new FlatLightLaf();
                    break;
                default:
                    throw new IllegalArgumentException(theme.name());
            }
        }
        setLaf(laf);
    }

    private static com.formdev.flatlaf.FlatLaf loadLafFromThemeFile(String themeFile) {
        LOGGER.info("Loading L&F theme from {}", themeFile);
        try (FileInputStream in = new FileInputStream(themeFile)) {
            return IntelliJTheme.createLaf(in);
        } catch (IOException e) {
            LOGGER.error("Couldn't load IntelliJ theme from file {}", themeFile, e);
            return null;
        }
    }

    private static void setLaf(com.formdev.flatlaf.FlatLaf lookAndFeel) {
        LOGGER.info("Setting L&F {}", lookAndFeel);
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            LOGGER.error("Couldn't set L&F", e);
        }
    }

    public static void setSystemLookAndFeel() {
        String systemLaf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(systemLaf);
        } catch (Exception e) {
            LOGGER.warn("Couldn't set system L&F {}", systemLaf, e);
        }
    }

    private static void setUIProperties(String uiPropertiesFile) {
        if (uiPropertiesFile == null) {
            LOGGER.debug("No UI properties file, skipping");
            return;
        }
        LOGGER.info("Setting addon properties file: {}", uiPropertiesFile);
        FlatLafAddon.PROPERTIES_FILE = uiPropertiesFile;
    }
}
