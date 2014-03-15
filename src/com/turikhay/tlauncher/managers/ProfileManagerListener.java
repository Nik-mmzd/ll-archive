package com.turikhay.tlauncher.managers;

import com.turikhay.tlauncher.minecraft.auth.AccountListener;

public interface ProfileManagerListener extends AccountListener {
   void onProfilesRefreshed(ProfileManager var1);

   void onProfileManagerChanged(ProfileManager var1);
}