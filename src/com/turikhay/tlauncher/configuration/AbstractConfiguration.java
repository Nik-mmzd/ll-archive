package com.turikhay.tlauncher.configuration;

import java.io.IOException;

public interface AbstractConfiguration {
   String get(String var1);

   int getInteger(String var1);

   double getDouble(String var1);

   float getFloat(String var1);

   long getLong(String var1);

   boolean getBoolean(String var1);

   String getDefault(String var1);

   int getDefaultInteger(String var1);

   double getDefaultDouble(String var1);

   float getDefaultFloat(String var1);

   long getDefaultLong(String var1);

   boolean getDefaultBoolean(String var1);

   void set(String var1, Object var2);

   void clear();

   void save() throws IOException;
}
