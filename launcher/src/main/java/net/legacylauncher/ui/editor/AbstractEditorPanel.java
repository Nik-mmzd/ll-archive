package net.legacylauncher.ui.editor;

import net.legacylauncher.ui.center.CenterPanel;
import net.legacylauncher.ui.center.CenterPanelTheme;
import net.legacylauncher.ui.images.ImageIcon;
import net.legacylauncher.ui.images.Images;
import net.legacylauncher.ui.loc.LocalizableLabel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEditorPanel extends CenterPanel {
    protected final List<EditorHandler> handlers;

    public AbstractEditorPanel(CenterPanelTheme theme, Insets insets) {
        super(theme, insets);
        handlers = new ArrayList<>();
    }

    public AbstractEditorPanel(Insets insets) {
        this(null, insets);
    }

    public AbstractEditorPanel() {
        this(null, null);
    }

    protected boolean checkValues() {
        boolean allValid = true;

        for (EditorHandler handler : handlers) {
            boolean valid = handler.isValid();
            setValid(handler, valid);
            if (!valid) {
                allValid = false;
            }
        }

        return allValid;
    }

    protected void setValid(EditorHandler handler, boolean valid) {
        Color color = valid ? getTheme().getBackground() : getTheme().getFailure();
        if (handler.getComponent() != null) {
            handler.getComponent().setOpaque(!valid);
            handler.getComponent().setBackground(color);
        }
    }

    protected JComponent createTip(String label, boolean warning) {
        LocalizableLabel tip = new LocalizableLabel(label);
        if (warning) {
            ImageIcon.setup(tip, Images.getIcon16("warning"));
        }

        return tip;
    }
}
