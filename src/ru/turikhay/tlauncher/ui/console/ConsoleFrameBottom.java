package ru.turikhay.tlauncher.ui.console;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import ru.turikhay.tlauncher.ui.images.Images;
import ru.turikhay.tlauncher.ui.loc.Localizable;
import ru.turikhay.tlauncher.ui.loc.LocalizableButton;
import ru.turikhay.tlauncher.ui.loc.LocalizableComponent;
import ru.turikhay.tlauncher.ui.swing.extended.BorderPanel;
import ru.turikhay.tlauncher.ui.swing.extended.ExtendedButton;
import ru.turikhay.tlauncher.ui.swing.extended.ExtendedPanel;
import ru.turikhay.util.MinecraftUtil;
import ru.turikhay.util.OS;
import ru.turikhay.util.SwingUtil;

public class ConsoleFrameBottom extends BorderPanel implements LocalizableComponent {
   private final ConsoleFrame frame;
   public final LocalizableButton closeCancelButton;
   public final ExtendedButton folder;
   public final ExtendedButton save;
   public final ExtendedButton pastebin;
   public final ExtendedButton kill;
   File openFolder;

   ConsoleFrameBottom(ConsoleFrame fr) {
      this.frame = fr;
      this.setOpaque(true);
      this.setBackground(Color.darkGray);
      this.closeCancelButton = new LocalizableButton("console.close.cancel");
      this.closeCancelButton.setVisible(false);
      this.closeCancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (ConsoleFrameBottom.this.closeCancelButton.isVisible()) {
               ConsoleFrameBottom.this.frame.hiding = false;
               ConsoleFrameBottom.this.closeCancelButton.setVisible(false);
            }

         }
      });
      this.setCenter(this.closeCancelButton);
      this.folder = this.newButton("folder.png", new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            OS.openFolder(ConsoleFrameBottom.this.openFolder == null ? MinecraftUtil.getWorkingDirectory() : ConsoleFrameBottom.this.openFolder);
         }
      });
      this.folder.setEnabled("Logger".equals(this.frame.console.getName()));
      this.save = this.newButton("document-save-as.png", new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ConsoleFrameBottom.this.frame.console.saveAs();
         }
      });
      this.pastebin = this.newButton("mail-attachment.png", new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ConsoleFrameBottom.this.frame.console.sendPaste();
         }
      });
      this.kill = this.newButton("process-stop.png", new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ConsoleFrameBottom.this.frame.console.launcher.killProcess();
            ConsoleFrameBottom.this.kill.setEnabled(false);
         }
      });
      this.kill.setEnabled(false);
      this.updateLocale();
      ExtendedPanel buttonPanel = new ExtendedPanel();
      buttonPanel.add(this.folder, this.save, this.pastebin, this.kill);
      this.setEast(buttonPanel);
   }

   private ExtendedButton newButton(String path, ActionListener action) {
      ExtendedButton button = new ExtendedButton();
      button.addActionListener(action);
      button.setIcon(Images.getIcon(path, SwingUtil.magnify(22), SwingUtil.magnify(22)));
      button.setPreferredSize(new Dimension(SwingUtil.magnify(32), SwingUtil.magnify(32)));
      return button;
   }

   public void updateLocale() {
      this.save.setToolTipText(Localizable.get("console.save"));
      this.pastebin.setToolTipText(Localizable.get("console.pastebin"));
      this.kill.setToolTipText(Localizable.get("console.kill"));
   }
}