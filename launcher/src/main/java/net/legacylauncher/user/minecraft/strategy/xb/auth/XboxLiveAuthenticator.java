package net.legacylauncher.user.minecraft.strategy.xb.auth;

import net.legacylauncher.user.minecraft.oauth.OAuthApplication;
import net.legacylauncher.user.minecraft.strategy.oatoken.MicrosoftOAuthToken;
import net.legacylauncher.user.minecraft.strategy.rqnpr.HttpClientRequester;
import net.legacylauncher.user.minecraft.strategy.rqnpr.InvalidResponseException;
import net.legacylauncher.user.minecraft.strategy.rqnpr.Requester;
import net.legacylauncher.user.minecraft.strategy.xb.XboxServiceAuthStrategy;
import net.legacylauncher.user.minecraft.strategy.xb.XboxServiceAuthenticationResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class XboxLiveAuthenticator extends XboxServiceAuthStrategy {
    private final static Logger LOGGER = LogManager.getLogger(XboxLiveAuthenticator.class);

    public XboxLiveAuthenticator(OAuthApplication application) {
        super(LOGGER, new HttpClientRequester<>(accessToken ->
                Request.Post("https://user.auth.xboxlive.com/user/authenticate")
                        .bodyString(
                                String.format(java.util.Locale.ROOT, "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"%s\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}",
                                        (application.usesWeirdXboxTokenPrefix() ? "d=" : "") + accessToken),
                                ContentType.APPLICATION_JSON
                        ))
        );
    }

    XboxLiveAuthenticator(Requester<String> requester) {
        super(LOGGER, requester);
    }

    public XboxServiceAuthenticationResponse xboxLiveAuthenticate(String accessToken)
            throws XboxLiveAuthenticationException, IOException {
        try {
            return requestAndParse(accessToken);
        } catch (InvalidResponseException e) {
            throw new XboxLiveAuthenticationException(e);
        }
    }

    public XboxServiceAuthenticationResponse xboxLiveAuthenticate(MicrosoftOAuthToken token)
            throws XboxLiveAuthenticationException, IOException {
        return xboxLiveAuthenticate(token.getAccessToken());
    }
}
