package net.legacylauncher.ui.swing;

import net.legacylauncher.util.OS;
import net.legacylauncher.util.U;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

public class UrlActionListener implements ActionListener {
    private final URL url;

    public UrlActionListener(String url) {
        this.url = U.makeURL(url, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OS.openLink(url);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("url", url)
                .build();
    }
}
