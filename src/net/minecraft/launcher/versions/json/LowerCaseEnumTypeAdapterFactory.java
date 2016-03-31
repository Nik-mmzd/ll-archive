package net.minecraft.launcher.versions.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LowerCaseEnumTypeAdapterFactory implements TypeAdapterFactory {
   public TypeAdapter create(Gson gson, TypeToken type) {
      Class rawType = type.getRawType();
      if (!rawType.isEnum()) {
         return null;
      } else {
         final Map lowercaseToConstant = new HashMap();
         Object[] var5 = rawType.getEnumConstants();
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            Object constant = var5[var7];
            lowercaseToConstant.put(toLowercase(constant), constant);
         }

         return new TypeAdapter() {
            public void write(JsonWriter out, Object value) throws IOException {
               if (value == null) {
                  out.nullValue();
               } else {
                  out.value(LowerCaseEnumTypeAdapterFactory.toLowercase(value));
               }

            }

            public Object read(JsonReader reader) throws IOException {
               if (reader.peek() == JsonToken.NULL) {
                  reader.nextNull();
                  return null;
               } else {
                  return lowercaseToConstant.get(reader.nextString());
               }
            }
         };
      }
   }

   private static String toLowercase(Object o) {
      return o.toString().toLowerCase(Locale.US);
   }
}
