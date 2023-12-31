package net.legacylauncher.ui.images;

import java.net.URL;
import java.util.Optional;

interface ImageResourceLocator {
    Optional<URL> loadResource(String resourceName);
}
