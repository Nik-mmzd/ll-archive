package net.legacylauncher.sentry;

import com.github.zafarkhaja.semver.Version;
import io.sentry.DefaultSentryClientFactory;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.context.ContextManager;
import io.sentry.context.SingletonContextManager;
import io.sentry.dsn.Dsn;
import io.sentry.event.EventBuilder;
import io.sentry.event.User;
import io.sentry.event.helper.EventBuilderHelper;
import net.legacylauncher.LegacyLauncher;
import net.legacylauncher.util.OS;
import net.legacylauncher.util.U;
import net.legacylauncher.util.windows.WMIProvider;

import java.util.UUID;

public class SentryConfigurer {
    private static SentryClient SENTRY;

    public static void configure(Version version, String shortBrand) {
        SENTRY = Sentry.init(
                LegacyLauncher.getInstance() != null && LegacyLauncher.getInstance().isDebug() ? null :
                        "https://6bd0f45848ad4217b1970ae598712dfc@sentry.ely.by/46",
                new CustomClientFactory()
        );
        SENTRY.setRelease(U.getNormalVersion(version));
        SENTRY.setEnvironment(shortBrand);
        SENTRY.setServerName(OS.CURRENT.name());
        SENTRY.addBuilderHelper(new CustomEventBuilderHelper());
    }

    public static void setUser(UUID uuid) {
        SENTRY.getContext().setUser(new User(
                uuid.toString(),
                null,
                null,
                null
        ));
    }

    private static class CustomClientFactory extends DefaultSentryClientFactory {
        private final ContextManager ctxManager = new SingletonContextManager();

        @Override
        protected ContextManager getContextManager(Dsn dsn) {
            return ctxManager;
        }
    }

    private static class CustomEventBuilderHelper implements EventBuilderHelper {
        @Override
        public void helpBuildingEvent(EventBuilder eventBuilder) {
            if (LegacyLauncher.getInstance() != null && LegacyLauncher.getInstance().getBootstrapVersion() != null) {
                eventBuilder.withTag("bootstrap", LegacyLauncher.getInstance().getBootstrapVersion());
            }
            eventBuilder.withTag("java", String.valueOf(OS.JAVA_VERSION.getMajor()));
            eventBuilder.withTag("java_version", System.getProperty("java.version"));
            eventBuilder.withTag("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
            eventBuilder.withTag("os_arch", System.getProperty("os.arch"));
            if (OS.WINDOWS.isCurrent()) {
                eventBuilder.withExtra("av", WMIProvider.getAvSoftwareList());
            }
        }
    }

    private SentryConfigurer() {
    }
}
