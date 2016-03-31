package ru.turikhay.util;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ru.turikhay.tlauncher.Bootstrapper;
import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.configuration.Configuration;
import ru.turikhay.util.async.ExtendedThread;
import ru.turikhay.util.stream.InputStringStream;

public class U {
   private static String PREFIX;
   public static final Object lock = new Object();
   private static Random rnd;

   public static void setPrefix(String prefix) {
      PREFIX = prefix;
   }

   public static void log(Object... what) {
      hlog(PREFIX, what);
   }

   public static void plog(Object... what) {
      hlog((String)null, what);
   }

   private static void hlog(String prefix, Object[] append) {
      Object var2 = lock;
      synchronized(lock) {
         System.out.println(toLog(prefix, append));
      }
   }

   private static String toLog(String prefix, Object... append) {
      StringBuilder b = new StringBuilder();
      boolean first = true;
      if (prefix != null) {
         b.append(prefix);
         first = false;
      }

      if (append != null) {
         Object[] var7 = append;
         int var6 = append.length;

         for(int var5 = 0; var5 < var6; ++var5) {
            Object e = var7[var5];
            if (e != null) {
               if (e.getClass().isArray()) {
                  if (!first) {
                     b.append(" ");
                  }

                  if (e instanceof Object[]) {
                     b.append(toLog((Object[])((Object[])e)));
                  } else {
                     b.append(arrayToLog(e));
                  }
                  continue;
               }

               if (e instanceof Throwable) {
                  if (!first) {
                     b.append("\n");
                  }

                  b.append(stackTrace((Throwable)e));
                  b.append("\n");
                  continue;
               }

               if (e instanceof File) {
                  if (!first) {
                     b.append(" ");
                  }

                  File en = (File)e;
                  String absPath = en.getAbsolutePath();
                  b.append(absPath);
                  if (en.isDirectory() && !absPath.endsWith(File.separator)) {
                     b.append(File.separator);
                  }
               } else if (e instanceof Iterator) {
                  Iterator var10 = (Iterator)e;

                  while(var10.hasNext()) {
                     b.append(" ");
                     b.append(toLog(var10.next()));
                  }
               } else if (e instanceof Enumeration) {
                  Enumeration var11 = (Enumeration)e;

                  while(var11.hasMoreElements()) {
                     b.append(" ");
                     b.append(toLog(var11.nextElement()));
                  }
               } else {
                  if (!first) {
                     b.append(" ");
                  }

                  b.append(e);
               }
            } else {
               if (!first) {
                  b.append(" ");
               }

               b.append("null");
            }

            if (first) {
               first = false;
            }
         }
      } else {
         b.append("null");
      }

      return b.toString();
   }

   public static String toLog(Object... append) {
      return toLog((String)null, append);
   }

   private static String arrayToLog(Object e) {
      if (!e.getClass().isArray()) {
         throw new IllegalArgumentException("Given object is not an array!");
      } else {
         StringBuilder b = new StringBuilder();
         boolean first = true;
         int var4;
         int var5;
         if (e instanceof Object[]) {
            Object[] var6;
            var5 = (var6 = (Object[])((Object[])e)).length;

            for(var4 = 0; var4 < var5; ++var4) {
               Object i = var6[var4];
               if (!first) {
                  b.append(" ");
               } else {
                  first = false;
               }

               b.append(i);
            }
         } else if (e instanceof int[]) {
            int[] var16;
            var5 = (var16 = (int[])((int[])e)).length;

            for(var4 = 0; var4 < var5; ++var4) {
               int var8 = var16[var4];
               if (!first) {
                  b.append(" ");
               } else {
                  first = false;
               }

               b.append(var8);
            }
         } else if (e instanceof boolean[]) {
            boolean[] var17;
            var5 = (var17 = (boolean[])((boolean[])e)).length;

            for(var4 = 0; var4 < var5; ++var4) {
               boolean var9 = var17[var4];
               if (!first) {
                  b.append(" ");
               } else {
                  first = false;
               }

               b.append(var9);
            }
         } else {
            int var18;
            if (e instanceof long[]) {
               long[] var7;
               var18 = (var7 = (long[])((long[])e)).length;

               for(var5 = 0; var5 < var18; ++var5) {
                  long var10 = var7[var5];
                  if (!first) {
                     b.append(" ");
                  } else {
                     first = false;
                  }

                  b.append(var10);
               }
            } else if (e instanceof float[]) {
               float[] var19;
               var5 = (var19 = (float[])((float[])e)).length;

               for(var4 = 0; var4 < var5; ++var4) {
                  float var11 = var19[var4];
                  if (!first) {
                     b.append(" ");
                  } else {
                     first = false;
                  }

                  b.append(var11);
               }
            } else if (e instanceof double[]) {
               double[] var20;
               var18 = (var20 = (double[])((double[])e)).length;

               for(var5 = 0; var5 < var18; ++var5) {
                  double var12 = var20[var5];
                  if (!first) {
                     b.append(" ");
                  } else {
                     first = false;
                  }

                  b.append(var12);
               }
            } else if (e instanceof byte[]) {
               byte[] var21;
               var5 = (var21 = (byte[])((byte[])e)).length;

               for(var4 = 0; var4 < var5; ++var4) {
                  byte var13 = var21[var4];
                  if (!first) {
                     b.append(" ");
                  } else {
                     first = false;
                  }

                  b.append(var13);
               }
            } else if (e instanceof short[]) {
               short[] var22;
               var5 = (var22 = (short[])((short[])e)).length;

               for(var4 = 0; var4 < var5; ++var4) {
                  short var14 = var22[var4];
                  if (!first) {
                     b.append(" ");
                  } else {
                     first = false;
                  }

                  b.append(var14);
               }
            } else if (e instanceof char[]) {
               char[] var23;
               var5 = (var23 = (char[])((char[])e)).length;

               for(var4 = 0; var4 < var5; ++var4) {
                  char var15 = var23[var4];
                  if (!first) {
                     b.append(" ");
                  } else {
                     first = false;
                  }

                  b.append(var15);
               }
            }
         }

         if (b.length() == 0) {
            throw new UnknownError("Unknown array type given.");
         } else {
            return b.toString();
         }
      }
   }

   public static void setLoadingStep(Bootstrapper.LoadingStep step) {
      if (step == null) {
         throw new NullPointerException();
      } else {
         plog("[Loading]", step.toString());
      }
   }

   public static double getAverage(double[] d, int max) {
      double a = 0.0D;
      int k = 0;
      double[] var9 = d;
      int var8 = d.length;

      for(int var7 = 0; var7 < var8; ++var7) {
         double curd = var9[var7];
         a += curd;
         ++k;
         if (k == max) {
            break;
         }
      }

      return k == 0 ? 0.0D : a / (double)k;
   }

   public static int getMaxMultiply(int i, int max) {
      if (i <= max) {
         return 1;
      } else {
         for(int x = max; x > 1; --x) {
            if (i % x == 0) {
               return x;
            }
         }

         return (int)Math.ceil((double)(i / max));
      }
   }

   public static String setFractional(double d, int fractional) {
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(fractional);
      return StringUtils.replaceChars(nf.format(d), ',', '.');
   }

   public static StringBuilder stackTrace(Throwable e) {
      StringBuilder trace = rawStackTrace(e);
      ExtendedThread currentAsExtended = (ExtendedThread)getAs(Thread.currentThread(), ExtendedThread.class);
      if (currentAsExtended != null) {
         trace.append("\nThread called by: ").append(rawStackTrace(currentAsExtended.getCaller()));
      }

      return trace;
   }

   private static StringBuilder rawStackTrace(Throwable e) {
      if (e == null) {
         return null;
      } else {
         StackTraceElement[] elems = e.getStackTrace();
         int programElements = 0;
         int totalElements = 0;
         StringBuilder builder = new StringBuilder();
         builder.append(e);
         StackTraceElement[] var8 = elems;
         int var7 = elems.length;

         for(int var6 = 0; var6 < var7; ++var6) {
            StackTraceElement cause = var8[var6];
            ++totalElements;
            String description = cause.toString();
            if (description.startsWith("ru.turikhay")) {
               ++programElements;
            }

            builder.append("\nat ").append(description);
            if (totalElements == 100 || programElements == 10) {
               int remain = elems.length - totalElements;
               if (remain != 0) {
                  builder.append("\n... and ").append(remain).append(" more");
               }
               break;
            }
         }

         Throwable var11 = e.getCause();
         if (var11 != null) {
            builder.append("\nCaused by: ").append(rawStackTrace(var11));
         }

         return builder;
      }
   }

   public static long getUsingSpace() {
      return getTotalSpace() - getFreeSpace();
   }

   public static long getFreeSpace() {
      return Runtime.getRuntime().freeMemory() / 1048576L;
   }

   public static long getTotalSpace() {
      return Runtime.getRuntime().totalMemory() / 1048576L;
   }

   public static String memoryStatus() {
      return getUsingSpace() + " / " + getTotalSpace() + " MB";
   }

   public static void gc() {
      log("Starting garbage collector: " + memoryStatus());
      System.gc();
      log("Garbage collector completed: " + memoryStatus());
   }

   public static void sleepFor(long millis) {
      try {
         Thread.sleep(millis);
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }

   public static URL makeURL(String p) {
      try {
         return new URL(p);
      } catch (Exception var2) {
         return null;
      }
   }

   public static URI makeURI(URL url) {
      try {
         return url.toURI();
      } catch (Exception var2) {
         return null;
      }
   }

   public static URI makeURI(String p) {
      return makeURI(makeURL(p));
   }

   public static int fitInterval(int val, int min, int max) {
      return val > max ? max : (val < min ? min : val);
   }

   public static int getReadTimeout() {
      return getConnectionTimeout();
   }

   public static int getConnectionTimeout() {
      TLauncher t = TLauncher.getInstance();
      if (t == null) {
         return 15000;
      } else {
         Configuration.ConnectionQuality quality = t.getSettings().getConnectionQuality();
         return quality == null ? 15000 : quality.getTimeout();
      }
   }

   public static Proxy getProxy() {
      return Proxy.NO_PROXY;
   }

   public static Object getRandom(Object[] array) {
      return array == null ? null : (array.length == 0 ? null : (array.length == 1 ? array[0] : array[(new Random()).nextInt(array.length)]));
   }

   public static LinkedHashMap sortMap(Map map, Object[] sortedKeys) {
      if (map == null) {
         return null;
      } else if (sortedKeys == null) {
         throw new NullPointerException("Keys cannot be NULL!");
      } else {
         LinkedHashMap result = new LinkedHashMap();
         Object[] var6 = sortedKeys;
         int var5 = sortedKeys.length;

         for(int var4 = 0; var4 < var5; ++var4) {
            Object key = var6[var4];
            Iterator var8 = map.entrySet().iterator();

            while(var8.hasNext()) {
               Entry entry = (Entry)var8.next();
               Object entryKey = entry.getKey();
               Object value = entry.getValue();
               if (key == null && entryKey == null) {
                  result.put((Object)null, value);
                  break;
               }

               if (key != null && key.equals(entryKey)) {
                  result.put(key, value);
                  break;
               }
            }
         }

         return result;
      }
   }

   public static Color shiftColor(Color color, int bits) {
      if (color == null) {
         return null;
      } else if (bits == 0) {
         return color;
      } else {
         int newRed = fitInterval(color.getRed() + bits, 0, 255);
         int newGreen = fitInterval(color.getGreen() + bits, 0, 255);
         int newBlue = fitInterval(color.getBlue() + bits, 0, 255);
         return new Color(newRed, newGreen, newBlue, color.getAlpha());
      }
   }

   public static Color shiftAlpha(Color color, int bits) {
      if (color == null) {
         return null;
      } else if (bits == 0) {
         return color;
      } else {
         int newAlpha = fitInterval(color.getAlpha() + bits, 0, 255);
         return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
      }
   }

   /** @deprecated */
   @Deprecated
   public static Object getAs(Object o, Class classOfT) {
      return Reflect.cast(o, classOfT);
   }

   public static boolean equal(Object a, Object b) {
      return a == b ? true : (a != null ? a.equals(b) : false);
   }

   public static void close(Closeable c) {
      try {
         c.close();
      } catch (Throwable var2) {
      }

   }

   public static int find(Object obj, Object[] array) {
      int i;
      if (obj == null) {
         for(i = 0; i < array.length; ++i) {
            if (array[i] == null) {
               return i;
            }
         }
      } else {
         for(i = 0; i < array.length; ++i) {
            if (obj.equals(array[i])) {
               return i;
            }
         }
      }

      return -1;
   }

   private static void swap(Object[] arr, int i, int j) {
      Object tmp = arr[i];
      arr[i] = arr[j];
      arr[j] = tmp;
   }

   public static Object[] shuffle(Object... arr) {
      if (rnd == null) {
         rnd = new Random();
      }

      int size = arr.length;

      for(int i = size; i > 1; --i) {
         swap(arr, i - 1, rnd.nextInt(i));
      }

      return arr;
   }

   public static String reverse(String s) {
      StringBuilder b = new StringBuilder();
      b.append(s);
      b.reverse();
      return b.toString();
   }

   public static byte[] toByteArray(String s) {
      try {
         return IOUtils.toByteArray(new InputStringStream(s));
      } catch (IOException var2) {
         throw new RuntimeException("i/o exception while reading string", var2);
      }
   }

   public static Object requireNotNull(Object obj, String name) {
      if (obj == null) {
         throw new NullPointerException(name);
      } else {
         return obj;
      }
   }
}
