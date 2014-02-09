package com.turikhay.tlauncher.ui.center;

import java.awt.Color;

public class DefaultCenterPanelTheme extends CenterPanelTheme {
   public final Color backgroundColor = new Color(255, 255, 255, 255);
   public final Color panelBackgroundColor = new Color(255, 255, 255, 128);
   public final Color focusColor = new Color(0, 0, 0, 255);
   public final Color focusLostColor = new Color(128, 128, 128, 255);
   public final Color successColor = Color.getHSBColor(0.25F, 0.66F, 0.66F);
   public final Color failureColor = Color.getHSBColor(0.0F, 0.3F, 1.0F);
   public final Color borderColor;
   public final Color delPanelColor;

   public DefaultCenterPanelTheme() {
      this.borderColor = this.successColor;
      this.delPanelColor = this.successColor;
   }

   public Color getBackground() {
      return this.backgroundColor;
   }

   public Color getPanelBackground() {
      return this.panelBackgroundColor;
   }

   public Color getFocus() {
      return this.focusColor;
   }

   public Color getFocusLost() {
      return this.focusLostColor;
   }

   public Color getSuccess() {
      return this.successColor;
   }

   public Color getFailure() {
      return this.failureColor;
   }

   public Color getBorder() {
      return this.borderColor;
   }

   public Color getDelPanel() {
      return this.delPanelColor;
   }
}