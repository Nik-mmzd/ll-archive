package ru.turikhay.tlauncher.ui.login;

import javax.swing.Box;
import javax.swing.BoxLayout;
import net.minecraft.launcher.updater.VersionSyncInfo;
import ru.turikhay.tlauncher.ui.alert.Alert;
import ru.turikhay.tlauncher.ui.block.BlockablePanel;
import ru.turikhay.tlauncher.ui.loc.LocalizableCheckbox;
import ru.turikhay.tlauncher.ui.swing.CheckBoxListener;
import ru.turikhay.util.U;
import ru.turikhay.util.async.AsyncThread;

public class CheckBoxPanel extends BlockablePanel implements LoginForm.LoginProcessListener {
   private static final long serialVersionUID = 768489049585749260L;
   private static final String[] phrases = new String[]{"This ain't easter egg."};
   public final LocalizableCheckbox autologin;
   public final LocalizableCheckbox forceupdate;
   private boolean state;
   private final LoginForm loginForm;

   CheckBoxPanel(LoginForm lf) {
      BoxLayout lm = new BoxLayout(this, 3);
      this.setLayout(lm);
      this.setOpaque(false);
      this.setAlignmentX(0.5F);
      this.loginForm = lf;
      this.autologin = new LocalizableCheckbox("loginform.checkbox.autologin", lf.global.getBoolean("login.auto"));
      this.autologin.addItemListener(new CheckBoxListener() {
         public void itemStateChanged(boolean newstate) {
            CheckBoxPanel.this.loginForm.autologin.setEnabled(newstate);
            if (newstate) {
               AsyncThread.execute(new Runnable() {
                  public void run() {
                     Alert.showLocMessage("loginform.checkbox.autologin.tip");
                  }
               });
            }

         }
      });
      this.forceupdate = new LocalizableCheckbox("loginform.checkbox.forceupdate");
      this.forceupdate.addItemListener(new CheckBoxListener() {
         private byte clicks = 0;

         public void itemStateChanged(boolean newstate) {
            if (++this.clicks == 10) {
               CheckBoxPanel.this.forceupdate.setText((String)U.getRandom(CheckBoxPanel.phrases));
               this.clicks = 0;
            }

            CheckBoxPanel.this.state = newstate;
            CheckBoxPanel.this.loginForm.buttons.play.updateState();
         }
      });
      this.add(this.autologin);
      this.add(Box.createHorizontalGlue());
      this.add(this.forceupdate);
   }

   public void logginingIn() throws LoginException {
      VersionSyncInfo syncInfo = this.loginForm.versions.getVersion();
      if (syncInfo != null) {
         boolean supporting = syncInfo.hasRemote();
         boolean installed = syncInfo.isInstalled();
         if (this.state) {
            if (!supporting) {
               Alert.showLocError("forceupdate.local");
               throw new LoginException("Cannot update local version!");
            }

            if (installed && !Alert.showLocQuestion("forceupdate.question")) {
               throw new LoginException("User has cancelled force updating.");
            }
         }
      }

   }

   public void loginFailed() {
   }

   public void loginSucceed() {
   }
}
