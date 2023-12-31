package net.legacylauncher.ui.swing.extended;

import net.legacylauncher.ui.block.Unblockable;

import java.awt.*;

public class UnblockablePanel extends ExtendedPanel implements Unblockable {
    public UnblockablePanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public UnblockablePanel(LayoutManager layout) {
        super(layout);
    }

    public UnblockablePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public UnblockablePanel() {
    }
}
