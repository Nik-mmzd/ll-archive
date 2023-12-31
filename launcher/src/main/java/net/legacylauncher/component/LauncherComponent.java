package net.legacylauncher.component;

import net.legacylauncher.managers.ComponentManager;

public abstract class LauncherComponent {
    protected final ComponentManager manager;

    public LauncherComponent(ComponentManager manager) {
        if (manager == null) {
            throw new NullPointerException();
        } else {
            this.manager = manager;
        }
    }
}
