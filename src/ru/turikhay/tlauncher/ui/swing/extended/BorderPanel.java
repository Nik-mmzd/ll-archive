package ru.turikhay.tlauncher.ui.swing.extended;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;

public class BorderPanel extends ExtendedPanel {
   private BorderPanel(BorderLayout layout, boolean isDoubleBuffered) {
      super(isDoubleBuffered);
      if (layout == null) {
         layout = new BorderLayout();
      }

      this.setLayout(layout);
   }

   public BorderPanel() {
      this((BorderLayout)null, true);
   }

   public BorderPanel(int hgap, int vgap) {
      this();
      this.setHgap(hgap);
      this.setVgap(vgap);
   }

   public BorderLayout getLayout() {
      return (BorderLayout)super.getLayout();
   }

   public void setLayout(LayoutManager mgr) {
      if (mgr instanceof BorderLayout) {
         super.setLayout(mgr);
      }

   }

   public void setHgap(int hgap) {
      this.getLayout().setHgap(hgap);
   }

   public void setVgap(int vgap) {
      this.getLayout().setVgap(vgap);
   }

   public void setNorth(Component comp) {
      this.add(comp, "North");
   }

   public void setEast(Component comp) {
      this.add(comp, "East");
   }

   public void setSouth(Component comp) {
      this.add(comp, "South");
   }

   public void setCenter(Component comp) {
      this.add(comp, "Center");
   }
}
