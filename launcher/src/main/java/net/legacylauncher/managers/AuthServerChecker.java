package net.legacylauncher.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import net.legacylauncher.ipc.ResolverIPC;
import net.legacylauncher.pasta.Pasta;
import net.legacylauncher.util.EHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthServerChecker implements ConnectivityManager.EntryChecker {
    private static final Logger LOGGER = LogManager.getLogger(AuthServerChecker.class);

    public static final String AUTH_SERVER_HOST = "authserver.mojang.com";
    private static final String AUTH_SERVER_STATUS_PAGE = "https://" + AUTH_SERVER_HOST;
    private static final String AUTH_SERVER_STATUS_OK = "OK";
    private final ResolverIPC resolver;

    private volatile DetectedThirdPartyAuthenticatorInfo detectedThirdPartyAuthenticator;

    public AuthServerChecker(ResolverIPC resolver) {
        this.resolver = resolver;
    }

    public DetectedThirdPartyAuthenticatorInfo getDetectedThirdPartyAuthenticator() {
        return detectedThirdPartyAuthenticator;
    }

    @Override
    public Boolean checkConnection() throws Exception {
        DetectedThirdPartyAuthenticatorInfo detectedAuthenticator = null;
        List<Callable<DetectedThirdPartyAuthenticatorInfo>> validators = Arrays.asList(
                this::queryDns,
                this::queryJsonStatus
        );
        for (Callable<DetectedThirdPartyAuthenticatorInfo> validator : validators) {
            detectedAuthenticator = validator.call();
            if (detectedAuthenticator != null) {
                break;
            }
        }
        if (detectedAuthenticator != null) {
            String name = detectedAuthenticator.getName();
            if (name == null) {
                LOGGER.warn("Detected unknown third-party authenticator");
            } else {
                LOGGER.warn("Detected third-party authenticator: {}", name);
            }
            Sentry.capture(new EventBuilder()
                    .withLevel(Event.Level.WARNING)
                    .withMessage(name == null ? "unknown third party authenticator" : "third party authenticator: " + name)
                    .withTag("authServerIps", authServerIps.stream().sorted().collect(Collectors.joining(", ")))
                    .withExtra("statusJson", Pasta.pasteJson(statusJsonString))
            );
            detectedThirdPartyAuthenticator = detectedAuthenticator;
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private List<String> authServerIps;

    private DetectedThirdPartyAuthenticatorInfo queryDns() throws IOException {
        InetAddress[] addresses = resolver.resolve(AUTH_SERVER_HOST);
        authServerIps = Arrays.stream(addresses).map(InetAddress::getHostAddress).collect(Collectors.toList());
        LOGGER.debug("{} resolves to {}", AUTH_SERVER_HOST, authServerIps);
        return Arrays.stream(addresses).flatMap(address -> {
            switch (address.getHostAddress()) {
                case "51.68.172.243":
                    return Stream.of(new DetectedThirdPartyAuthenticatorInfo("EasyMC"));
                case "134.209.132.125":
                    return Stream.of(new DetectedThirdPartyAuthenticatorInfo("MCLeaks"));
                case "142.44.142.126":
                    return Stream.of(new DetectedThirdPartyAuthenticatorInfo("The Altening"));
                case "127.0.0.1":
                    // most likely a third-party authenticator, too
                    return Stream.of(new DetectedThirdPartyAuthenticatorInfo(null));
                default:
                    return Stream.empty();
            }
        }).findAny().orElse(null);
    }

    private DetectedThirdPartyAuthenticatorInfo queryJsonStatus() throws Exception {
        try {
            queryJsonStatusUsingClient(EHttpClient.getGlobalClient());
        } catch (SSLException e) {
            if (!(e.getCause() instanceof CertificateException)) {
                LOGGER.warn("Couldn't query json status because of SSL error, but it is not caused by validation failure");
                throw e;
            }
            LOGGER.warn("Couldn't query json status because of SSL validation error", e);
            LOGGER.debug("Now trying with unsafe http client");
            try {
                return queryJsonStatusUsingUnsafeClient(e);
            } catch (Exception e1) {
                e1.addSuppressed(e);
                throw e1;
            }
        }
        // no third-party authenticator detected
        return null;
    }

    private DetectedThirdPartyAuthenticatorInfo queryJsonStatusUsingUnsafeClient(SSLException e) throws Exception {
        CloseableHttpClient unsafeHttpsClient;
        try {
            unsafeHttpsClient = createUnsafeHttpsClient();
        } catch (Exception e1) {
            LOGGER.warn("Couldn't create unsafe http client", e1);
            throw e;
        }
        try {
            queryJsonStatusUsingClient(unsafeHttpsClient);
        } catch (UnexpectedStatus e1) {
            // request was made -> probably third-party authenticator
            return new DetectedThirdPartyAuthenticatorInfo(null);
        } catch (IOException e1) {
            LOGGER.debug("Unsafe http client request also failed");
            throw e1;
        }
        LOGGER.warn("Unsafe http client request succeeded. Probably third party authenticator");
        return new DetectedThirdPartyAuthenticatorInfo(null);
    }

    private String statusJsonString;

    private void queryJsonStatusUsingClient(HttpClient httpClient) throws IOException, UnexpectedStatus {
        this.statusJsonString = EHttpClient.toString(httpClient, Request.Get(AUTH_SERVER_STATUS_PAGE));
        LOGGER.debug("Status page ({}) returned: {}", AUTH_SERVER_STATUS_PAGE, statusJsonString);
        String status;
        try {
            JsonElement statusJson = JsonParser.parseString(statusJsonString);
            status = statusJson.getAsJsonObject().getAsJsonPrimitive("Status").getAsString();
        } catch (RuntimeException parseException) {
            throw new UnexpectedStatus(parseException);
        }
        if (!AUTH_SERVER_STATUS_OK.equals(status)) {
            LOGGER.warn("Status is not {}: \"{}\"", AUTH_SERVER_STATUS_OK, status);
            throw new UnexpectedStatus(status);
        }
    }

    public static ConnectivityManager.Entry createEntry(ResolverIPC resolver) {
        return new ConnectivityManager.Entry(AUTH_SERVER_HOST, AUTH_SERVER_HOST, new AuthServerChecker(resolver));
    }

    private static CloseableHttpClient createUnsafeHttpsClient() throws Exception {
        return EHttpClient.builder()
                .setSSLContext(new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build()
                )
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
    }

    public static class DetectedThirdPartyAuthenticatorInfo {
        private final String name;

        DetectedThirdPartyAuthenticatorInfo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static class UnexpectedStatus extends Exception {
        public UnexpectedStatus(String status) {
            super(status, null, true, false);
        }

        public UnexpectedStatus(Throwable cause) {
            super(null, cause, true, false);
        }
    }
}
