package net.minecraft.launcher.versions;

import net.legacylauncher.repository.Repository;
import net.minecraft.launcher.updater.VersionList;

import java.util.Date;

public interface Version {
    String getID();

    void setID(String var1);

    String getUrl();

    String getJar();

    ReleaseType getReleaseType();

    String getType();

    Repository getSource();

    void setSource(Repository var1);

    Date getUpdatedTime();

    Date getReleaseTime();

    VersionList getVersionList();

    void setVersionList(VersionList var1);
}