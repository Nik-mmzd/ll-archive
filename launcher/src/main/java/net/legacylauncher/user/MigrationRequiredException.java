package net.legacylauncher.user;

public class MigrationRequiredException extends AuthException {
    MigrationRequiredException() {
        super("Account requires migration", "mojang-need-migration");
        this.softException = true;
    }
}
