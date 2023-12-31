package net.legacylauncher.ui.settings;

import net.legacylauncher.ui.LegacyLauncherFrame;
import net.legacylauncher.ui.block.Blocker;
import net.legacylauncher.ui.editor.EditorField;
import net.legacylauncher.ui.editor.EditorIntegerField;
import net.legacylauncher.ui.loc.LocalizableLabel;
import net.legacylauncher.ui.swing.extended.BorderPanel;
import net.legacylauncher.ui.swing.extended.ExtendedPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SettingsFontSlider extends BorderPanel implements EditorField {
    private final JSlider slider = new JSlider();
    private final EditorIntegerField inputField;

    SettingsFontSlider() {
        slider.setOpaque(false);
        slider.setMinimum((int) LegacyLauncherFrame.minFontSize);
        slider.setMaximum((int) LegacyLauncherFrame.maxFontSize);
        slider.setMinorTickSpacing(2);
        slider.setMajorTickSpacing(4);
        slider.setSnapToTicks(true);
        slider.setPaintTicks(true);
        slider.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        setCenter(slider);
        inputField = new EditorIntegerField();
        inputField.textField.setColumns(2);
        LocalizableLabel pt = new LocalizableLabel("settings.fontsize.pt");
        ExtendedPanel panel = new ExtendedPanel();
        panel.add(inputField, pt);
        setEast(panel);
        slider.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                onSliderUpdate();
            }
        });
        slider.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                onSliderUpdate();
            }
        });
        inputField.textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateInfo();
            }

            public void removeUpdate(DocumentEvent e) {
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    public void setBackground(Color color) {
        if (inputField != null) {
            inputField.setBackground(color);
        }

    }

    public void block(Object reason) {
        Blocker.blockComponents(reason, slider, inputField);
    }

    public void unblock(Object reason) {
        Blocker.unblockComponents(reason, slider, inputField);
    }

    public String getSettingsValue() {
        return inputField.textField.getValue();
    }

    public void setSettingsValue(String value) {
        inputField.textField.setValue(value);
        updateInfo();
    }

    public boolean isValueValid() {
        return inputField.getIntegerValue() >= 12;
    }

    private void onSliderUpdate() {
        inputField.textField.setValue(slider.getValue());
    }

    private void updateSlider() {
        int intVal = inputField.getIntegerValue();
        if (intVal > 1) {
            slider.setValue(intVal);
        }
    }

    private void updateInfo() {
        updateSlider();
    }
}
