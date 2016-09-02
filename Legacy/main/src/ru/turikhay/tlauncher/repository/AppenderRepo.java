package ru.turikhay.tlauncher.repository;

import org.apache.commons.lang3.StringUtils;
import ru.turikhay.util.U;

import java.io.IOException;
import java.net.URL;

public class AppenderRepo extends Repo {
    private final String prefix, suffix;

    public AppenderRepo(String prefix, String suffix) {
        super(prefix);

        if (StringUtils.isEmpty(prefix)) {
            throw new IllegalArgumentException("prefix is empty");
        }
        if (StringUtils.isEmpty(suffix)) {
            suffix = null;
        }

        this.prefix = prefix;
        this.suffix = suffix;
    }

    public AppenderRepo(String prefix) {
        this(prefix, null);
    }

    @Override
    protected URL makeUrl(String path) throws IOException {
        String url;

        if (suffix == null) {
            url = prefix + path;
        } else {
            url = prefix + path + suffix;
        }

        return new URL(url);
    }

    static AppenderRepo[] fromString(String... arr) {
        if (U.requireNotNull(arr).length == 0) {
            return new AppenderRepo[0];
        }

        AppenderRepo[] result = new AppenderRepo[U.requireNotNull(arr).length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = new AppenderRepo(arr[i]);
        }
        return result;
    }
}
