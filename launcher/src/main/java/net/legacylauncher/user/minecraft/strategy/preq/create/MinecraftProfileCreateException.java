package net.legacylauncher.user.minecraft.strategy.preq.create;

import net.legacylauncher.user.minecraft.strategy.MinecraftAuthenticationException;

public class MinecraftProfileCreateException extends MinecraftAuthenticationException {
    public MinecraftProfileCreateException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getShortKey() {
        return "minecraft_profile_create";
    }
}
