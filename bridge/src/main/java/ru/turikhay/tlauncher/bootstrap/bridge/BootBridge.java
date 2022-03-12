package ru.turikhay.tlauncher.bootstrap.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BootBridge {
    final ArrayList<BootListener> listenerList = new ArrayList<BootListener>();
    private BootEventDispatcher dispatcher;

    private final String bootstrapVersion;
    private final String[] args;
    private final String options;
    private final Map<String, BootMessage> messageMap;
    private UUID client;

    volatile boolean interrupted;

    private BootBridge(String bootstrapVersion, String[] args, String options) {
        if(args == null) {
            args = new String[0];
        }

        this.messageMap = new HashMap<String, BootMessage>();

        this.bootstrapVersion = bootstrapVersion;
        this.args = args;
        this.options = options;
    }

    public String getBootstrapVersion() {
        return bootstrapVersion;
    }

    public String[] getArgs() {
        return args;
    }

    public String getOptions() {
        return options;
    }

    public UUID getClient() {
        return client;
    }

    public void setInterrupted() {
        interrupted = true;
    }

    void setClient(UUID client) {
        this.client = client;
    }

    BootMessage getMessage(String locale) {
        return messageMap.get(locale);
    }

    public void addMessage(String locale, String title, String message) {
        messageMap.put(locale, new BootMessage(title, message));
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

    public static BootBridge create(String bootstrapVersion, String[] args, String options) {
        return new BootBridge(bootstrapVersion, args, options);
    }

    public static BootBridge create(String[] args) {
        return new BootBridge(null, args, null);
    }
}
