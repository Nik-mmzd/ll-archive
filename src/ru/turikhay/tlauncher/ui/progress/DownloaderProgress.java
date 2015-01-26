package ru.turikhay.tlauncher.ui.progress;

import java.awt.Component;
import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.downloader.Downloadable;
import ru.turikhay.tlauncher.downloader.Downloader;
import ru.turikhay.tlauncher.downloader.DownloaderListener;
import ru.turikhay.tlauncher.ui.loc.LocalizableProgressBar;

public class DownloaderProgress extends LocalizableProgressBar implements DownloaderListener {
   private static final long serialVersionUID = -8382205925341380876L;

   private DownloaderProgress(Component parentComp, Downloader downloader) {
      super(parentComp);
      if (downloader == null) {
         throw new NullPointerException();
      } else {
         downloader.addListener(this);
         this.stopProgress();
      }
   }

   public DownloaderProgress(Component parentComp) {
      this(parentComp, TLauncher.getInstance().getDownloader());
   }

   public void onDownloaderStart(Downloader d, int files) {
      this.startProgress();
      this.setIndeterminate(true);
      this.setCenterString("progressBar.init");
      this.setEastString("progressBar.downloading", new Object[]{files});
   }

   public void onDownloaderAbort(Downloader d) {
      this.stopProgress();
   }

   public void onDownloaderProgress(Downloader d, double dprogress, double speed) {
      if (dprogress > 0.0D) {
         int progress = (int)(dprogress * 100.0D);
         if (this.getValue() > progress) {
            return;
         }

         this.setIndeterminate(false);
         this.setValue(progress);
         this.setCenterString(progress + "%");
      }

   }

   public void onDownloaderFileComplete(Downloader d, Downloadable file) {
      this.setIndeterminate(false);
      this.setEastString("progressBar.remaining", new Object[]{d.getRemaining()});
   }

   public void onDownloaderComplete(Downloader d) {
      this.stopProgress();
   }
}
