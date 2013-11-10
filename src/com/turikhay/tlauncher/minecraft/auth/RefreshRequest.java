package com.turikhay.tlauncher.minecraft.auth;

public class RefreshRequest extends Request {
   private String clientToken;
   private String accessToken;
   private GameProfile selectedProfile;

   RefreshRequest(String clientToken, String accessToken, GameProfile profile) {
      this.clientToken = clientToken;
      this.accessToken = accessToken;
      this.selectedProfile = profile;
   }

   RefreshRequest(String clientToken, String accessToken) {
      this(clientToken, accessToken, (GameProfile)null);
   }

   RefreshRequest(Authenticator auth, GameProfile profile) {
      this(auth.getClientToken(), auth.getAccessToken(), profile);
   }

   RefreshRequest(Authenticator auth) {
      this((Authenticator)auth, (GameProfile)null);
   }

   public String getClientToken() {
      return this.clientToken;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public GameProfile getProfile() {
      return this.selectedProfile;
   }
}