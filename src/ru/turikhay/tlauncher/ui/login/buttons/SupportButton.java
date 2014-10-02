package ru.turikhay.tlauncher.ui.login.buttons;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import javax.swing.JPopupMenu;
import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.ui.block.Blockable;
import ru.turikhay.tlauncher.ui.loc.LocalizableComponent;
import ru.turikhay.tlauncher.ui.loc.LocalizableMenuItem;
import ru.turikhay.tlauncher.ui.login.LoginForm;
import ru.turikhay.tlauncher.ui.swing.ImageButton;
import ru.turikhay.util.OS;
import ru.turikhay.util.U;

public class SupportButton extends ImageButton implements Blockable, LocalizableComponent {
   private SupportButton.SupportType type;

   SupportButton(LoginForm loginForm) {
      this.rotation = ImageButton.ImageRotation.CENTER;
      this.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            SupportButton.this.type.popupMenu.show(SupportButton.this, 0, SupportButton.this.getHeight());
         }
      });
      this.updateLocale();
   }

   public SupportButton.SupportType getType() {
      return this.type;
   }

   public void setType(SupportButton.SupportType type) {
      if (type == null) {
         throw new NullPointerException("type");
      } else {
         this.type = type;
         this.setImage(type.image);
         this.repaint();
      }
   }

   public void updateLocale() {
      String locale = TLauncher.getInstance().getSettings().getLocale().toString();
      this.setType(!locale.equals("ru_RU") && !locale.equals("uk_UA") ? SupportButton.SupportType.GMAIL : SupportButton.SupportType.VK);
   }

   public void block(Object reason) {
   }

   public void unblock(Object reason) {
   }

   public static enum SupportType {
      VK("vk.png") {
         public void setupMenu() {
            this.popupMenu.add(SupportButton.SupportType.newItem("loginform.button.support.follow", new ActionListener() {
               final URI followURI = U.makeURI("http://goo.gl/zOBYu6");

               public void actionPerformed(ActionEvent e) {
                  OS.openLink(this.followURI);
               }
            }));
            this.popupMenu.add(SupportButton.SupportType.newItem("loginform.button.support.report", new ActionListener() {
               final URI reportURI = U.makeURI("http://goo.gl/NBlzdI");

               public void actionPerformed(ActionEvent e) {
                  OS.openLink(this.reportURI);
               }
            }));
         }
      },
      GMAIL("mail.png") {
         public void setupMenu() {
            this.popupMenu.add(SupportButton.SupportType.newItem("loginform.button.support.email", new ActionListener() {
               final URI devURI = U.makeURI("http://turikhay.ru/");

               public void actionPerformed(ActionEvent e) {
                  OS.openLink(this.devURI);
               }
            }));
         }
      };

      protected final JPopupMenu popupMenu;
      private final Image image;

      private SupportType(String imagePath) {
         this.popupMenu = new JPopupMenu();
         this.image = SupportButton.loadImage(imagePath);
         this.setupMenu();
      }

      public Image getImage() {
         return this.image;
      }

      public abstract void setupMenu();

      private static final LocalizableMenuItem newItem(String key, ActionListener action) {
         LocalizableMenuItem item = new LocalizableMenuItem(key);
         item.addActionListener(action);
         return item;
      }

      // $FF: synthetic method
      SupportType(String var3, SupportButton.SupportType var4) {
         this(var3);
      }
   }
}
