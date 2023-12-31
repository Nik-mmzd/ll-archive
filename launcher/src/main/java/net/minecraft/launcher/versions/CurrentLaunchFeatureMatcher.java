package net.minecraft.launcher.versions;

import net.legacylauncher.LegacyLauncher;

public class CurrentLaunchFeatureMatcher implements Rule.FeatureMatcher {

    public CurrentLaunchFeatureMatcher() {
    }

    @Override
    public boolean hasFeature(String key, Object value) {
        if (key != null) {
            switch (key) {
                case "has_custom_resolution":
                    int[] clientSize = LegacyLauncher.getInstance().getSettings().getClientWindowSize();
                    if (value != null && "true".equals(value.toString()) && clientSize[0] > 0 && clientSize[1] > 0) {
                        return true;
                    }
                    break;
                case "is_demo_user":
                    return false;
            }
        }
        return false;
    }
}
