package com.turikhay.tlauncher.ui.progress;

import com.turikhay.tlauncher.minecraft.crash.Crash;
import com.turikhay.tlauncher.minecraft.launcher.MinecraftException;
import com.turikhay.tlauncher.minecraft.launcher.MinecraftExtendedListener;
import java.awt.Component;

public class LaunchProgress extends DownloaderProgress implements MinecraftExtendedListener {
   private static final long serialVersionUID = -1003141285749311799L;

   public LaunchProgress(Component parentComp) {
      super(parentComp);
   }

   public void clearProgress() {
      this.setIndeterminate(false);
      this.setValue(0);
      this.setCenterString((String)null);
      this.setEastString((String)null);
   }

   private void setupBar() {
      this.startProgress();
      this.setIndeterminate(true);
   }

   public void onMinecraftPrepare() {
      this.setupBar();
   }

   public void onMinecraftCollecting() {
      this.setWestString("launcher.step.collecting");
   }

   public void onMinecraftComparingAssets() {
      this.setWestString("launcher.step.comparing-assets");
   }

   public void onMinecraftDownloading() {
      this.setWestString("launcher.step.downloading");
   }

   public void onMinecraftReconstructingAssets() {
      this.setupBar();
      this.setWestString("launcher.step.reconstructing-assets");
   }

   public void onMinecraftUnpackingNatives() {
      this.setWestString("launcher.step.unpacking-natives");
   }

   public void onMinecraftDeletingEntries() {
      this.setWestString("launcher.step.deleting-entries");
   }

   public void onMinecraftConstructing() {
      this.setWestString("launcher.step.constructing");
   }

   public void onMinecraftPostLaunch() {
   }

   public void onMinecraftAbort() {
      this.stopProgress();
   }

   public void onMinecraftLaunch() {
      this.stopProgress();
   }

   public void onMinecraftClose() {
   }

   public void onMinecraftError(Throwable e) {
      this.stopProgress();
   }

   public void onMinecraftKnownError(MinecraftException e) {
      this.stopProgress();
   }

   public void onMinecraftCrash(Crash crash) {
      this.stopProgress();
   }
}