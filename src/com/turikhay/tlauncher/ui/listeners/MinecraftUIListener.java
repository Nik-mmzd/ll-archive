package com.turikhay.tlauncher.ui.listeners;

import com.turikhay.tlauncher.TLauncher;
import com.turikhay.tlauncher.configuration.LangConfiguration;
import com.turikhay.tlauncher.minecraft.crash.Crash;
import com.turikhay.tlauncher.minecraft.crash.CrashSignatureContainer;
import com.turikhay.tlauncher.minecraft.launcher.MinecraftException;
import com.turikhay.tlauncher.minecraft.launcher.MinecraftListener;
import com.turikhay.tlauncher.ui.alert.Alert;
import com.turikhay.tlauncher.ui.loc.Localizable;
import com.turikhay.util.U;
import java.io.File;
import java.net.URI;
import java.util.Iterator;
import net.minecraft.launcher.OperatingSystem;

public class MinecraftUIListener implements MinecraftListener {
   private final TLauncher t;
   private final LangConfiguration lang;

   public MinecraftUIListener(TLauncher tlauncher) {
      this.t = tlauncher;
      this.lang = this.t.getLang();
   }

   public void onMinecraftPrepare() {
   }

   public void onMinecraftAbort() {
   }

   public void onMinecraftLaunch() {
      this.t.hide();
      this.t.getVersionManager().asyncRefresh();
      if (this.t.getUpdater() != null) {
         this.t.getUpdater().asyncFindUpdate();
      }

   }

   public void onMinecraftClose() {
      this.t.show();
      if (this.t.getUpdater() != null) {
         this.t.getUpdaterListener().applyDelayedUpdate();
      }

   }

   public void onMinecraftCrash(Crash crash) {
      String p = "crash.";
      String title = Localizable.get(p + "title");
      String report = crash.getFile();
      if (!crash.isRecognized()) {
         Alert.showError(title, Localizable.get(p + "unknown"), (Throwable)null);
      } else {
         Iterator var6 = crash.getSignatures().iterator();

         while(var6.hasNext()) {
            CrashSignatureContainer.CrashSignature sign = (CrashSignatureContainer.CrashSignature)var6.next();
            String path = sign.getPath();
            String message = Localizable.get(p + path);
            String url = Localizable.get(p + path + ".url");
            URI uri = U.makeURI(url);
            if (uri != null) {
               if (Alert.showQuestion(title, message, report, false)) {
                  OperatingSystem.openLink(uri);
               }
            } else {
               Alert.showMessage(title, message, report);
            }
         }
      }

      if (report != null) {
         if (Alert.showQuestion(p + "store", false)) {
            U.log("Removing crash report...");
            File file = new File(report);
            if (!file.exists()) {
               U.log("File is already removed. LOL.");
            } else {
               try {
                  if (!file.delete()) {
                     throw new Exception("file.delete() returned false");
                  }
               } catch (Exception var11) {
                  U.log("Can't delete crash report file. Okay.");
                  Alert.showAsyncMessage(p + "store.failed", var11);
                  return;
               }

               U.log("Yay, crash report file doesn't exist by now.");
            }

            Alert.showAsyncMessage(p + "store.success");
         }

      }
   }

   public void onMinecraftError(Throwable e) {
      Alert.showError(this.lang.get("launcher.error.title"), this.lang.get("launcher.error.unknown"), e);
   }

   public void onMinecraftKnownError(MinecraftException e) {
      Alert.showError(this.lang.get("launcher.error.title"), this.lang.get("launcher.error." + e.getLangPath(), e.getLangVars()), e.getCause());
   }
}
