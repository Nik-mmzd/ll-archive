package net.legacylauncher;

import net.legacylauncher.ipc.*;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.freedesktop.dbus.connections.impl.DirectConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyLauncherEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyLauncherEntrypoint.class);
    public static void launchP2P(String busAddress) throws DBusException {
        DirectConnection connection = DirectConnectionBuilder.forAddress(busAddress).build();
        launch(new DBusConnectionForwarder.Direct(connection));
    }

    public static void launchSession(String busName) throws DBusException {
        DBusConnection connection = DBusConnectionBuilder.forSessionBus().build();
        try {
            connection.requestBusName("net.legacylauncher.LegacyLauncher");
        } catch (DBusException e) {
            // not critical, well-known names are not used in real ipc communications
            LOGGER.warn("Unable to request well-known name on the bus", e);
        }
        launch(new DBusConnectionForwarder.Bus(connection, busName));
    }

    private static void launch(DBusConnectionForwarder connection) throws DBusException {
        // make sure bootstrap onto a bus
        connection.getRemoteObject(Bootstrap1.OBJECT_PATH, Peer.class).Ping();

        ResolverIPC resolver;
        try {
            Resolver1 resolver1 = connection.getRemoteObject(Resolver1.OBJECT_PATH, Resolver1.class);
            resolver1.Ping();
            resolver = new DBusResolverIPC(connection, resolver1);
        } catch (DBusExecutionException e) {
            resolver = SystemDefaultResolverIPC.INSTANCE;
        }
        DBusBootstrapIPC ipc = new DBusBootstrapIPC(connection);
        ipc.register(connection);
        LegacyLauncher.launch(ipc, resolver);
    }


    public static void main(String... args) throws DBusException {
        if (args.length < 1) {
            throw new IllegalStateException("at least one argument with a bus address required");
        }

        if (args[0].startsWith(SESSION_PREFIX)) {
            launchSession(args[0].substring(SESSION_PREFIX.length()));
        } else {
            launchP2P(args[0]);
        }
    }

    private static final String SESSION_PREFIX = "session:";
}
