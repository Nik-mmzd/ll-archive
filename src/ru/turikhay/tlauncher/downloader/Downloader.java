package ru.turikhay.tlauncher.downloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.configuration.Configuration;
import ru.turikhay.util.U;
import ru.turikhay.util.async.ExtendedThread;

public class Downloader extends ExtendedThread {
   public static final int MAX_THREADS = 6;
   public static final String DOWNLOAD_BLOCK = "download";
   static final String ITERATION_BLOCK = "iteration";
   private final DownloaderThread[] threads;
   private final List list;
   private final List listeners;
   private Configuration.ConnectionQuality configuration;
   private final AtomicInteger remainingObjects;
   private int runningThreads;
   private int workingThreads;
   private final double[] speedContainer;
   private final double[] progressContainer;
   private double lastAverageProgress;
   private double averageProgress;
   private double averageSpeed;
   private boolean aborted;
   private final Object workLock;

   private Downloader(Configuration.ConnectionQuality configuration) {
      super("MD");
      this.setConfiguration(configuration);
      this.remainingObjects = new AtomicInteger();
      this.threads = new DownloaderThread[6];
      this.list = Collections.synchronizedList(new ArrayList());
      this.listeners = Collections.synchronizedList(new ArrayList());
      this.speedContainer = new double[6];
      this.progressContainer = new double[6];
      this.workLock = new Object();
      this.startAndWait();
   }

   public Downloader(TLauncher tlauncher) {
      this(tlauncher.getSettings().getConnectionQuality());
   }

   public Configuration.ConnectionQuality getConfiguration() {
      return this.configuration;
   }

   public int getRemaining() {
      return this.remainingObjects.get();
   }

   public double getProgress() {
      return this.averageProgress;
   }

   public double getSpeed() {
      return this.averageSpeed;
   }

   public void add(Downloadable d) {
      if (d == null) {
         throw new NullPointerException();
      } else {
         this.list.add(d);
      }
   }

   public void add(DownloadableContainer c) {
      if (c == null) {
         throw new NullPointerException();
      } else {
         this.list.addAll(c.list);
      }
   }

   public void addAll(Downloadable... ds) {
      if (ds == null) {
         throw new NullPointerException();
      } else {
         for(int i = 0; i < ds.length; ++i) {
            if (ds[i] == null) {
               throw new NullPointerException("Downloadable at " + i + " is NULL!");
            }

            this.list.add(ds[i]);
         }

      }
   }

   public void addAll(Collection coll) {
      if (coll == null) {
         throw new NullPointerException();
      } else {
         int i = -1;
         Iterator var4 = coll.iterator();

         while(var4.hasNext()) {
            Downloadable d = (Downloadable)var4.next();
            ++i;
            if (d == null) {
               throw new NullPointerException("Downloadable at" + i + " is NULL!");
            }

            this.list.add(d);
         }

      }
   }

   public void addListener(DownloaderListener listener) {
      if (listener == null) {
         throw new NullPointerException();
      } else {
         this.listeners.add(listener);
      }
   }

   public boolean startDownload() {
      boolean haveWork = !this.list.isEmpty();
      if (haveWork) {
         this.unblockThread("iteration");
      }

      return haveWork;
   }

   public void startDownloadAndWait() {
      if (this.startDownload()) {
         this.waitWork();
      }

   }

   private void waitWork() {
      synchronized(this.workLock) {
         try {
            this.workLock.wait();
         } catch (InterruptedException var3) {
         }

      }
   }

   private void notifyWork() {
      synchronized(this.workLock) {
         this.workLock.notifyAll();
      }
   }

   public void stopDownload() {
      if (!this.isThreadBlocked()) {
         throw new IllegalStateException();
      } else {
         for(int i = 0; i < this.runningThreads; ++i) {
            this.threads[i].stopDownload();
         }

         this.aborted = true;
         this.unblockThread("download");
      }
   }

   public void stopDownloadAndWait() {
      this.stopDownload();
      this.waitForThreads();
   }

   public void setConfiguration(Configuration.ConnectionQuality configuration) {
      if (configuration == null) {
         throw new NullPointerException();
      } else {
         log("Loaded configuration:", configuration);
         this.configuration = configuration;
      }
   }

   public void run() {
      this.checkCurrent();

      while(true) {
         this.blockThread("iteration");
         log("Files in queue", this.list.size());
         synchronized(this.list) {
            this.sortOut();
         }

         for(int i = 0; i < this.runningThreads; ++i) {
            this.threads[i].startDownload();
         }

         this.blockThread("download");
         if (this.aborted) {
            this.waitForThreads();
            this.onAbort();
            this.aborted = false;
         }

         this.notifyWork();
         Arrays.fill(this.speedContainer, 0.0D);
         Arrays.fill(this.progressContainer, 0.0D);
         this.averageProgress = 0.0D;
         this.lastAverageProgress = 0.0D;
         this.workingThreads = 0;
         this.remainingObjects.set(0);
      }
   }

   private void sortOut() {
      int size = this.list.size();
      if (size != 0) {
         int downloadablesAtThread = U.getMaxMultiply(size, 6);
         int x = 0;
         int y = true;
         log("Starting download " + size + " files...");
         this.onStart(size);
         int max = this.configuration.getMaxThreads();

         boolean[] workers;
         for(workers = new boolean[max]; size > 0; downloadablesAtThread = U.getMaxMultiply(size, 6)) {
            for(int i = 0; i < max; ++i) {
               workers[i] = true;
               size -= downloadablesAtThread;
               if (this.threads[i] == null) {
                  this.threads[i] = new DownloaderThread(this, ++this.runningThreads);
               }

               int y;
               for(y = x; y < x + downloadablesAtThread; ++y) {
                  this.threads[i].add((Downloadable)this.list.get(y));
               }

               x = y;
               if (size == 0) {
                  break;
               }
            }
         }

         boolean[] var10 = workers;
         int var9 = workers.length;

         for(int var8 = 0; var8 < var9; ++var8) {
            boolean worker = var10[var8];
            if (worker) {
               ++this.workingThreads;
            }
         }

         this.list.clear();
      }
   }

   private void onStart(int size) {
      Iterator var3 = this.listeners.iterator();

      while(var3.hasNext()) {
         DownloaderListener listener = (DownloaderListener)var3.next();
         listener.onDownloaderStart(this, size);
      }

      this.remainingObjects.addAndGet(size);
   }

   void onAbort() {
      Iterator var2 = this.listeners.iterator();

      while(var2.hasNext()) {
         DownloaderListener listener = (DownloaderListener)var2.next();
         listener.onDownloaderAbort(this);
      }

   }

   void onProgress(DownloaderThread thread, double curprogress, double curspeed) {
      int id = thread.getID() - 1;
      this.progressContainer[id] = curprogress;
      this.speedContainer[id] = curspeed;
      this.averageProgress = U.getAverage(this.progressContainer, this.workingThreads);
      if (!(this.averageProgress - this.lastAverageProgress < 0.01D)) {
         this.lastAverageProgress = this.averageProgress;
         this.averageSpeed = U.getSum(this.speedContainer);
         Iterator var8 = this.listeners.iterator();

         while(var8.hasNext()) {
            DownloaderListener listener = (DownloaderListener)var8.next();
            listener.onDownloaderProgress(this, this.averageProgress, this.averageSpeed);
         }

      }
   }

   void onFileComplete(DownloaderThread thread, Downloadable file) {
      int remaining = this.remainingObjects.decrementAndGet();
      Iterator var5 = this.listeners.iterator();

      while(var5.hasNext()) {
         DownloaderListener listener = (DownloaderListener)var5.next();
         listener.onDownloaderFileComplete(this, file);
      }

      if (remaining == 0) {
         this.onComplete();
      }

   }

   private void onComplete() {
      Iterator var2 = this.listeners.iterator();

      while(var2.hasNext()) {
         DownloaderListener listener = (DownloaderListener)var2.next();
         listener.onDownloaderComplete(this);
      }

      this.unblockThread("download");
   }

   private void waitForThreads() {
      log("Waiting for", this.workingThreads, "threads...");

      boolean blocked;
      do {
         blocked = true;

         for(int i = 0; i < this.workingThreads; ++i) {
            if (!this.threads[i].isThreadBlocked()) {
               blocked = false;
            }
         }
      } while(!blocked);

      log("All threads are blocked by now");
   }

   private static void log(Object... o) {
      U.log("[Downloader2]", o);
   }
}
