package net.legacylauncher.managers;

public interface VersionManagerListener {
    void onVersionsRefreshing(VersionManager var1);

    void onVersionsRefreshingFailed(VersionManager var1);

    void onVersionsRefreshed(VersionManager var1);
}
