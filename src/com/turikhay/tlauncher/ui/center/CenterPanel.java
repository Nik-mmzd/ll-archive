package com.turikhay.tlauncher.ui.center;

import com.turikhay.tlauncher.TLauncher;
import com.turikhay.tlauncher.configuration.Configuration;
import com.turikhay.tlauncher.configuration.LangConfiguration;
import com.turikhay.tlauncher.ui.Del;
import com.turikhay.tlauncher.ui.awt.UnmodifiableInsets;
import com.turikhay.tlauncher.ui.block.BlockablePanel;
import com.turikhay.tlauncher.ui.loc.LocalizableLabel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class CenterPanel extends BlockablePanel {
   private static final long serialVersionUID = -1975869198322761508L;
   public static final CenterPanelTheme defaultTheme = new DefaultCenterPanelTheme();
   public static final CenterPanelTheme tipTheme = new TipPanelTheme();
   public static final Insets defaultInsets = new UnmodifiableInsets(5, 24, 18, 24);
   public static final Insets squareInsets = new UnmodifiableInsets(15, 15, 15, 15);
   public static final Insets smallSquareInsets = new UnmodifiableInsets(7, 7, 7, 7);
   private final Insets insets;
   private final CenterPanelTheme theme;
   protected final JPanel messagePanel;
   protected final LocalizableLabel messageLabel;
   public final TLauncher tlauncher;
   public final Configuration global;
   public final LangConfiguration lang;

   public CenterPanel() {
      this((CenterPanelTheme)null, (Insets)null);
   }

   public CenterPanel(Insets insets) {
      this((CenterPanelTheme)null, insets);
   }

   public CenterPanel(CenterPanelTheme theme, Insets insets) {
      this.tlauncher = TLauncher.getInstance();
      this.global = this.tlauncher.getSettings();
      this.lang = this.tlauncher.getLang();
      CenterPanelTheme var10001 = theme == null ? defaultTheme : theme;
      theme = var10001;
      this.theme = var10001;
      this.insets = insets == null ? defaultInsets : insets;
      this.setLayout(new BoxLayout(this, 3));
      this.setBackground(theme.getPanelBackground());
      this.setOpaque(false);
      this.messageLabel = new LocalizableLabel("  ");
      this.messageLabel.setFont(this.getFont().deriveFont(1));
      this.messageLabel.setVerticalAlignment(0);
      this.messageLabel.setHorizontalTextPosition(0);
      this.messagePanel = sepPan(new FlowLayout(), this.messageLabel);
      this.add(Box.createVerticalGlue());
   }

   public void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D)g0;
      int arc = 32;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(this.getBackground());
      g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), arc, arc);
      g.setColor(this.theme.getBorder());

      for(int x = 1; x < 2; ++x) {
         g.drawRoundRect(x - 1, x - 1, this.getWidth() - 2 * x + 1, this.getHeight() - 2 * x + 1, arc, arc);
      }

      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      super.paintComponent(g);
   }

   public CenterPanelTheme getTheme() {
      return this.theme;
   }

   public Insets getInsets() {
      return this.insets;
   }

   public Del del(int aligment) {
      return new Del(1, aligment, this.theme.getBorder());
   }

   public Del del(int aligment, int width, int height) {
      return new Del(1, aligment, width, height, this.theme.getBorder());
   }

   public void defocus() {
      this.requestFocusInWindow();
   }

   public boolean setError(String message) {
      this.messageLabel.setForeground(this.theme.getFailure());
      this.messageLabel.setText(message != null && message.length() != 0 ? message : " ");
      return false;
   }

   public boolean setMessage(String message) {
      this.messageLabel.setForeground(this.theme.getSuccess());
      this.messageLabel.setText(message != null && message.length() != 0 ? message : " ");
      return true;
   }

   public static JPanel sepPan(LayoutManager manager, Component... components) {
      BlockablePanel panel = new BlockablePanel(manager);
      panel.setOpaque(false);
      Component[] var6 = components;
      int var5 = components.length;

      for(int var4 = 0; var4 < var5; ++var4) {
         Component comp = var6[var4];
         panel.add(comp);
      }

      return panel;
   }

   public static JPanel sepPan(Component... components) {
      return sepPan(new GridLayout(0, 1), components);
   }
}