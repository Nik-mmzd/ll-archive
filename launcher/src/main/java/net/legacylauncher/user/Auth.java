package net.legacylauncher.user;

import java.io.IOException;

public interface Auth<T extends User> {
    void validate(T user) throws AuthException, IOException;
}
