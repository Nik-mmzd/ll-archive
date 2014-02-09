package com.turikhay.tlauncher.ui.loc;

import javax.swing.JLabel;

public class LocalizableLabel extends JLabel implements LocalizableComponent {
   private static final long serialVersionUID = 7628068160047735335L;
   private String path;
   private String[] variables;

   public LocalizableLabel(String path, Object... vars) {
      this.setOpaque(false);
      this.setText(path, vars);
   }

   public LocalizableLabel(String path) {
      this(path, Localizable.EMPTY_VARS);
   }

   public LocalizableLabel() {
      this((String)null);
   }

   public LocalizableLabel(int horizontalAlignment) {
      this((String)null);
      this.setHorizontalAlignment(horizontalAlignment);
   }

   public void setText(String path, Object... vars) {
      this.path = path;
      this.variables = Localizable.checkVariables(vars);
      String value = Localizable.get(path);

      for(int i = 0; i < this.variables.length; ++i) {
         value = value.replace("%" + i, this.variables[i]);
      }

      super.setText(value);
   }

   public void setText(String path) {
      this.setText(path, Localizable.EMPTY_VARS);
   }

   public void updateLocale() {
      this.setText(this.path, this.variables);
   }
}