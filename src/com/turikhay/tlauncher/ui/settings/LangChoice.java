package com.turikhay.tlauncher.ui.settings;

import com.turikhay.tlauncher.TLauncher;
import com.turikhay.tlauncher.ui.loc.Localizable;
import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class LangChoice extends Choice implements SettingsField {
   private static final long serialVersionUID = 570709181645184106L;
   private boolean saveable = true;
   private Map replacer = new LinkedHashMap();
   private String current;
   boolean changed;

   LangChoice(SettingsForm sf) {
      Locale[] available = Localizable.get().getLocales();
      Locale[] var6 = available;
      int var5 = available.length;

      for(int var4 = 0; var4 < var5; ++var4) {
         Locale loc = var6[var4];
         String id = loc.toString();
         String curdisp = loc.getDisplayCountry(Locale.ENGLISH) + " (" + id + ")";
         this.replacer.put(curdisp, id);
         this.add(curdisp);
         if (TLauncher.getInstance().getSettings().getLocale() == loc) {
            this.select(curdisp);
         }
      }

      this.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            LangChoice.this.changed = !e.getItem().equals(LangChoice.this.current);
         }
      });
   }

   public String getValue() {
      return (String)this.replacer.get(this.getSelectedItem());
   }

   public void setValue(String id) {
      Iterator var3 = this.replacer.entrySet().iterator();

      while(var3.hasNext()) {
         Entry curen = (Entry)var3.next();
         if (((String)curen.getKey()).equals(id)) {
            this.select((String)curen.getValue());
         }
      }

      this.setCurrent();
   }

   public void setCurrent() {
      this.changed = false;
      this.current = this.getSelectedItem();
   }

   public String getSettingsPath() {
      return "locale";
   }

   public boolean isValueValid() {
      return true;
   }

   public void setToDefault() {
   }

   public boolean isSaveable() {
      return this.saveable;
   }

   public void setSaveable(boolean val) {
      this.saveable = val;
   }
}