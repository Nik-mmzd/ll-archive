package net.legacylauncher.user;

import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import net.legacylauncher.user.minecraft.oauth.OAuthApplication;
import net.legacylauncher.user.minecraft.strategy.MinecraftAuthenticationException;
import net.legacylauncher.user.minecraft.strategy.gos.GameOwnershipValidationException;
import net.legacylauncher.user.minecraft.strategy.gos.GameOwnershipValidator;
import net.legacylauncher.user.minecraft.strategy.mcsauth.MinecraftServicesAuthenticationException;
import net.legacylauncher.user.minecraft.strategy.mcsauth.MinecraftServicesAuthenticator;
import net.legacylauncher.user.minecraft.strategy.mcsauth.MinecraftServicesToken;
import net.legacylauncher.user.minecraft.strategy.oatoken.MicrosoftOAuthToken;
import net.legacylauncher.user.minecraft.strategy.oatoken.refresh.MicrosoftOAuthTokenRefreshException;
import net.legacylauncher.user.minecraft.strategy.oatoken.refresh.MicrosoftOAuthTokenRefresher;
import net.legacylauncher.user.minecraft.strategy.preq.MinecraftOAuthProfile;
import net.legacylauncher.user.minecraft.strategy.preq.MinecraftProfileRequestException;
import net.legacylauncher.user.minecraft.strategy.preq.MinecraftProfileRequester;
import net.legacylauncher.user.minecraft.strategy.xb.XboxServiceAuthenticationResponse;
import net.legacylauncher.user.minecraft.strategy.xb.auth.XboxLiveAuthenticationException;
import net.legacylauncher.user.minecraft.strategy.xb.auth.XboxLiveAuthenticator;
import net.legacylauncher.user.minecraft.strategy.xb.xsts.XSTSAuthenticationException;
import net.legacylauncher.user.minecraft.strategy.xb.xsts.XSTSAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MinecraftAuth implements Auth<MinecraftUser> {
    private static final Logger LOGGER = LogManager.getLogger(MinecraftAuth.class);
    private final OAuthApplication APP = OAuthApplication.TL;

    @Override
    public void validate(MinecraftUser user) throws AuthException, IOException {
        if (user.getMinecraftToken().isExpired()) {
            refreshMinecraftAccessToken(user);
        }
        MinecraftProfileRequester requester = new MinecraftProfileRequester();
        MinecraftOAuthProfile profile;
        try {
            profile = requester.requestProfile(user.getMinecraftToken());
        } catch (MinecraftProfileRequestException e) {
            throw wrap(e);
        } catch (IOException ioE) {
            captureIOException("requesting profile", ioE);
            throw ioE;
        }
        LOGGER.info("Profile validated: {}", profile);
        user.setProfile(profile);
    }

    private void refreshMinecraftAccessToken(MinecraftUser user) throws AuthException, IOException {
        if (user.getMicrosoftToken().isExpired()) {
            refreshMicrosoftAccessToken(user);
        }
        XboxServiceAuthenticationResponse xstsToken = authenticateOnXboxServices(user);
        MinecraftServicesAuthenticator mcsAuthenticator = new MinecraftServicesAuthenticator();
        MinecraftServicesToken mcsToken;
        try {
            mcsToken = mcsAuthenticator.minecraftServicesAuthenticate(xstsToken);
        } catch (MinecraftServicesAuthenticationException e) {
            throw wrap(e);
        } catch (IOException ioE) {
            captureIOException("mcs auth", ioE);
            throw ioE;
        }
        GameOwnershipValidator ownershipValidator = new GameOwnershipValidator();
        try {
            ownershipValidator.checkGameOwnership(mcsToken);
        } catch (GameOwnershipValidationException e) {
            throw wrap(e);
        } catch (IOException ioE) {
            captureIOException("checking ownership", ioE);
            throw ioE;
        }
        user.setMinecraftToken(mcsToken);
    }

    private XboxServiceAuthenticationResponse authenticateOnXboxServices(MinecraftUser user)
            throws AuthException, IOException {
        XboxLiveAuthenticator xboxLiveAuthenticator = new XboxLiveAuthenticator(APP);
        XboxServiceAuthenticationResponse xboxLiveToken;
        try {
            xboxLiveToken = xboxLiveAuthenticator.xboxLiveAuthenticate(user.getMicrosoftToken());
        } catch (XboxLiveAuthenticationException e) {
            throw wrap(e);
        } catch (IOException ioE) {
            captureIOException("xbox live auth", ioE);
            throw ioE;
        }
        XSTSAuthenticator xstsAuthenticator = new XSTSAuthenticator();
        XboxServiceAuthenticationResponse xstsToken;
        try {
            xstsToken = xstsAuthenticator.xstsAuthenticate(xboxLiveToken.getToken());
        } catch (XSTSAuthenticationException e) {
            throw wrap(e);
        } catch (IOException ioE) {
            captureIOException("xsts auth", ioE);
            throw ioE;
        }
        return xstsToken;
    }

    private void refreshMicrosoftAccessToken(MinecraftUser user) throws AuthException, IOException {
        MicrosoftOAuthTokenRefresher refresher = new MicrosoftOAuthTokenRefresher(APP);
        MicrosoftOAuthToken token;
        try {
            token = refresher.refreshToken(user.getMicrosoftToken());
        } catch (MicrosoftOAuthTokenRefreshException e) {
            throw wrap(e);
        } catch (IOException ioE) {
            captureIOException("refreshing msft", ioE);
            throw ioE;
        }
        user.setMicrosoftToken(token);
    }

    private static AuthException wrap(MinecraftAuthenticationException e) {
        LOGGER.error("Couldn't validate the user", e);
        Sentry.capture(new EventBuilder()
                .withLevel(Event.Level.ERROR)
                .withSentryInterface(new ExceptionInterface(e))
                .withMessage("couldn't validate Microsoft user")
        );
        return new AuthException(e.toString(), e.getShortKey());
    }

    private static void captureIOException(String phase, IOException e) {
        Sentry.capture(new EventBuilder()
                .withLevel(Event.Level.ERROR)
                .withSentryInterface(new ExceptionInterface(e))
                .withMessage("i/o validating Microsoft user while " + phase)
        );
    }
}
