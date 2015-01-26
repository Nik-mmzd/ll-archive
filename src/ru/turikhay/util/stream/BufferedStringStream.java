package ru.turikhay.util.stream;

public class BufferedStringStream extends StringStream {
   protected int caretFlush;

   public void write(char b) {
      super.write(b);
      if (b == '\n') {
         this.flush();
         this.caretFlush = this.caret;
      }
   }

   public void clear() {
      super.clear();
      this.caret = 0;
      this.caretFlush = 0;
   }
}
