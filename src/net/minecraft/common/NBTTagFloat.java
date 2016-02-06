package net.minecraft.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagFloat extends NBTBase {
   public float data;

   public NBTTagFloat(String par1Str) {
      super(par1Str);
   }

   void write(DataOutput par1DataOutput) throws IOException {
      par1DataOutput.writeFloat(this.data);
   }

   void load(DataInput par1DataInput, int par2) throws IOException {
      this.data = par1DataInput.readFloat();
   }

   public byte getId() {
      return 5;
   }

   public String toString() {
      return "" + this.data;
   }

   public boolean equals(Object par1Obj) {
      if (super.equals(par1Obj)) {
         NBTTagFloat var2 = (NBTTagFloat)par1Obj;
         return this.data == var2.data;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return super.hashCode() ^ Float.floatToIntBits(this.data);
   }
}
