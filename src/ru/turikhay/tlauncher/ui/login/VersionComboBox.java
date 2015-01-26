package ru.turikhay.tlauncher.ui.login;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.swing.ListCellRenderer;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.CompleteVersion;
import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.managers.VersionManager;
import ru.turikhay.tlauncher.managers.VersionManagerListener;
import ru.turikhay.tlauncher.ui.alert.Alert;
import ru.turikhay.tlauncher.ui.block.Blockable;
import ru.turikhay.tlauncher.ui.loc.LocalizableComponent;
import ru.turikhay.tlauncher.ui.swing.SimpleComboBoxModel;
import ru.turikhay.tlauncher.ui.swing.VersionCellRenderer;
import ru.turikhay.tlauncher.ui.swing.extended.ExtendedComboBox;

public class VersionComboBox extends ExtendedComboBox implements Blockable, VersionManagerListener, LocalizableComponent, LoginForm.LoginProcessListener {
   private static final long serialVersionUID = -9122074452728842733L;
   static boolean showElyVersions;
   private static final VersionSyncInfo LOADING;
   private static final VersionSyncInfo EMPTY;
   private final VersionManager manager;
   private final LoginForm loginForm;
   private final SimpleComboBoxModel model;
   private String selectedVersion;

   static {
      LOADING = VersionCellRenderer.LOADING;
      EMPTY = VersionCellRenderer.EMPTY;
   }

   VersionComboBox(LoginForm lf) {
      super((ListCellRenderer)(new VersionCellRenderer() {
         public boolean getShowElyVersions() {
            return VersionComboBox.showElyVersions;
         }
      }));
      this.loginForm = lf;
      this.model = this.getSimpleModel();
      this.manager = TLauncher.getInstance().getVersionManager();
      this.manager.addListener(this);
      this.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            VersionComboBox.this.loginForm.buttons.play.updateState();
            VersionSyncInfo selected = VersionComboBox.this.getVersion();
            if (selected != null) {
               VersionComboBox.this.selectedVersion = selected.getID();
            }

         }
      });
      this.selectedVersion = lf.global.get("login.version");
   }

   public VersionSyncInfo getVersion() {
      VersionSyncInfo selected = (VersionSyncInfo)this.getSelectedItem();
      return selected != null && !selected.equals(LOADING) && !selected.equals(EMPTY) ? selected : null;
   }

   public void logginingIn() throws LoginException {
      VersionSyncInfo selected = this.getVersion();
      if (selected == null) {
         throw new LoginWaitException("Version list is empty, refreshing", new LoginWaitException.LoginWaitTask() {
            public void runTask() throws LoginException {
               VersionComboBox.this.manager.refresh();
               if (VersionComboBox.this.getVersion() == null) {
                  Alert.showLocError("versions.notfound");
               }

               throw new LoginException("Giving user a second chance to choose correct version...");
            }
         });
      } else if (selected.hasRemote() && selected.isInstalled() && !selected.isUpToDate()) {
         if (!Alert.showLocQuestion("versions.found-update")) {
            try {
               CompleteVersion complete = this.manager.getLocalList().getCompleteVersion(selected.getLocal());
               complete.setUpdatedTime(selected.getLatestVersion().getUpdatedTime());
               this.manager.getLocalList().saveVersion(complete);
            } catch (IOException var3) {
               Alert.showLocError("versions.found-update.error");
            }

         } else {
            this.loginForm.checkbox.forceupdate.setSelected(true);
         }
      }
   }

   public void loginFailed() {
   }

   public void loginSucceed() {
   }

   public void updateLocale() {
      this.updateList(this.manager.getVersions(), (String)null);
   }

   public void onVersionsRefreshing(VersionManager vm) {
      this.updateList((List)null, (String)null);
   }

   public void onVersionsRefreshingFailed(VersionManager vm) {
      this.updateList(this.manager.getVersions(), (String)null);
   }

   public void onVersionsRefreshed(VersionManager vm) {
      this.updateList(this.manager.getVersions(), (String)null);
   }

   void updateList(List list, String select) {
      if (select == null && this.selectedVersion != null) {
         select = this.selectedVersion;
      }

      this.removeAllItems();
      if (list == null) {
         this.addItem(LOADING);
      } else {
         if (list.isEmpty()) {
            this.addItem(EMPTY);
         } else {
            this.model.addElements(list);
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
               VersionSyncInfo version = (VersionSyncInfo)var4.next();
               if (select != null && version.getID().equals(select)) {
                  this.setSelectedItem(version);
               }
            }
         }

      }
   }

   public void block(Object reason) {
      this.setEnabled(false);
   }

   public void unblock(Object reason) {
      this.setEnabled(true);
   }
}
