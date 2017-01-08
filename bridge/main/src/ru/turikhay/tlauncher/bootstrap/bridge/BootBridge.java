package ru.turikhay.tlauncher.bootstrap.bridge;

import javax.swing.*;
import java.net.URLDecoder;
import java.util.ArrayList;

public final class BootBridge {
    final ArrayList<BootListener> listenerList = new ArrayList<BootListener>();
    private BootEventDispatcher dispatcher;

    private final String bootstrapVersion;
    private final String[] args;
    private final String updateInfo;

    private BootBridge(String bootstrapVersion, String[] args, String updateInfo) {
        if(args == null) {
            args = new String[0];
        }

        this.bootstrapVersion = bootstrapVersion;
        this.args = args;
        this.updateInfo = updateInfo;
    }

    public String getBootstrapVersion() {
        return bootstrapVersion;
    }

    public String[] getArgs() {
        return args;
    }

    public String getUpdateInfo() {
        return updateInfo;
    }

    public synchronized void addListener(BootListener listener) {
        if(listener == null) {
            throw new NullPointerException("listener");
        }
        listenerList.add(listener);
    }

    public synchronized BootEventDispatcher setupDispatcher() {
        if(this.dispatcher != null) {
            throw new IllegalStateException("dispatcher already set");
        }
        return (this.dispatcher = new BootEventDispatcher(this));
    }

    public void waitUntilClose() throws InterruptedException, BootException {
        if(this.dispatcher == null) {
            throw new IllegalStateException("no dispatcher initialized");
        }
        this.dispatcher.waitUntilClose();
    }

    public static BootBridge create(String bootstrapVersion, String[] args, String updateInfo) {
        return new BootBridge(bootstrapVersion, args, updateInfo);
    }

    public static BootBridge create(String[] args) {
        return new BootBridge(null, args, null);
    }
}
