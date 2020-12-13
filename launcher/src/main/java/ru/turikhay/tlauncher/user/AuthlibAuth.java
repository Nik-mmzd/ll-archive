package ru.turikhay.tlauncher.user;

import ru.turikhay.tlauncher.connection.ConnectionHelper;
import ru.turikhay.util.StringUtil;
import ru.turikhay.util.U;

import java.io.IOException;
import java.util.UUID;

public abstract class AuthlibAuth<T extends AuthlibUser> implements Auth<T> {

    public com.mojang.authlib.UserAuthentication authorize(String clientToken, String username, String password) throws AuthException, IOException {
        StringUtil.requireNotBlank(clientToken, "clientToken");
        StringUtil.requireNotBlank(username, "username");
        StringUtil.requireNotBlank(password, "password");

        com.mojang.authlib.UserAuthentication userAuthentication = createUserAuthentication(clientToken);
        userAuthentication.setUsername(username);
        userAuthentication.setPassword(password);

        logIn(userAuthentication);

        if(userAuthentication.getSelectedProfile() == null) {
            throw new InvalidCredentialsException("no selected profile");
        }

        return userAuthentication;
    }

    public void validate(T user) throws AuthException, IOException {
        logIn(U.requireNotNull(user, "user").getMojangUserAuthentication());
    }

    protected void logIn(com.mojang.authlib.UserAuthentication userAuthentication) throws AuthException, IOException {
        try {
            U.requireNotNull(userAuthentication, "user").logIn();
        } catch (com.mojang.authlib.exceptions.UserMigratedException userMigrated) {
            throw new AuthException(userMigrated, "migrated");
        } catch (com.mojang.authlib.exceptions.InvalidCredentialsException invalidCredentials) {
            throw new InvalidCredentialsException(invalidCredentials.getMessage());
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            if(ConnectionHelper.fixCertException(e, "mojang-auth") == -1) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new AuthUnknownException(e);
            }
        }
    }

    protected com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService createYggdrasilAuthenticationService(String clientToken) {
        return new com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService(U.getProxy(), StringUtil.requireNotBlank(clientToken));
    }

    protected com.mojang.authlib.UserAuthentication createUserAuthentication(String clientToken) {
        return createYggdrasilAuthenticationService(clientToken).createUserAuthentication(com.mojang.authlib.Agent.MINECRAFT);
    }

    protected static String randomClientToken() {
        return String.valueOf(UUID.randomUUID());
    }
}