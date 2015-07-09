package ru.turikhay.tlauncher.minecraft.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AuthenticatorDatabase {
   private final Map accounts;
   private AccountListener listener;

   public AuthenticatorDatabase(Map map) {
      if (map == null) {
         throw new NullPointerException();
      } else {
         this.accounts = map;
      }
   }

   public AuthenticatorDatabase() {
      this(new LinkedHashMap());
   }

   public Collection getAccounts() {
      return Collections.unmodifiableCollection(this.accounts.values());
   }

   public Account getByUUID(String uuid) {
      return (Account)this.accounts.get(uuid);
   }

   public Account getByUsername(String username, Account.AccountType type) {
      if (username == null) {
         throw new NullPointerException();
      } else {
         Iterator var4 = this.accounts.values().iterator();

         Account acc;
         do {
            do {
               if (!var4.hasNext()) {
                  return null;
               }

               acc = (Account)var4.next();
            } while(!username.equals(acc.getUsername()));
         } while(type != null && type != acc.getType());

         return acc;
      }
   }

   public void registerAccount(Account acc) {
      if (acc == null) {
         throw new NullPointerException();
      } else if (this.accounts.containsValue(acc)) {
         throw new IllegalArgumentException("This account already exists!");
      } else {
         String uuid = acc.getUUID() == null ? acc.getUsername() : acc.getUUID();
         this.accounts.put(uuid, acc);
         this.fireRefresh();
      }
   }

   public void unregisterAccount(Account acc) {
      if (acc == null) {
         throw new NullPointerException();
      } else if (!this.accounts.containsValue(acc)) {
         throw new IllegalArgumentException("This account doesn't exist!");
      } else {
         this.accounts.values().remove(acc);
         this.fireRefresh();
      }
   }

   private void fireRefresh() {
      if (this.listener != null) {
         this.listener.onAccountsRefreshed(this);
      }
   }

   public void setListener(AccountListener listener) {
      this.listener = listener;
   }

   public static class Serializer implements JsonDeserializer, JsonSerializer {
      public AuthenticatorDatabase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         Map services = new LinkedHashMap();
         Map credentials = this.deserializeCredentials((JsonObject)json, context);
         Iterator var7 = credentials.entrySet().iterator();

         while(var7.hasNext()) {
            Entry en = (Entry)var7.next();
            services.put((String)en.getKey(), new Account((Map)en.getValue()));
         }

         return new AuthenticatorDatabase(services);
      }

      Map deserializeCredentials(JsonObject json, JsonDeserializationContext context) {
         Map result = new LinkedHashMap();
         Iterator var5 = json.entrySet().iterator();

         while(var5.hasNext()) {
            Entry authEntry = (Entry)var5.next();
            Map credentials = new LinkedHashMap();
            Iterator var8 = ((JsonObject)authEntry.getValue()).entrySet().iterator();

            while(var8.hasNext()) {
               Entry credentialsEntry = (Entry)var8.next();
               credentials.put((String)credentialsEntry.getKey(), this.deserializeCredential((JsonElement)credentialsEntry.getValue()));
            }

            result.put((String)authEntry.getKey(), credentials);
         }

         return result;
      }

      private Object deserializeCredential(JsonElement element) {
         Iterator var4;
         if (element instanceof JsonObject) {
            Map result = new LinkedHashMap();
            var4 = ((JsonObject)element).entrySet().iterator();

            while(var4.hasNext()) {
               Entry entry = (Entry)var4.next();
               result.put((String)entry.getKey(), this.deserializeCredential((JsonElement)entry.getValue()));
            }

            return result;
         } else if (!(element instanceof JsonArray)) {
            return element.getAsString();
         } else {
            List result = new ArrayList();
            var4 = ((JsonArray)element).iterator();

            while(var4.hasNext()) {
               JsonElement entry = (JsonElement)var4.next();
               result.add(this.deserializeCredential(entry));
            }

            return result;
         }
      }

      public JsonElement serialize(AuthenticatorDatabase src, Type typeOfSrc, JsonSerializationContext context) {
         Map services = src.accounts;
         Map credentials = new LinkedHashMap();
         Iterator var7 = services.entrySet().iterator();

         while(var7.hasNext()) {
            Entry en = (Entry)var7.next();
            credentials.put((String)en.getKey(), ((Account)en.getValue()).createMap());
         }

         return context.serialize(credentials);
      }
   }
}
