package net.legacylauncher.ui.versions;

import net.legacylauncher.managers.VersionManager;
import net.legacylauncher.ui.block.Blocker;
import net.legacylauncher.ui.center.CenterPanel;
import net.legacylauncher.ui.images.Images;
import net.legacylauncher.ui.loc.LocalizableLabel;
import net.legacylauncher.ui.swing.ScrollPane;
import net.legacylauncher.ui.swing.SimpleListModel;
import net.legacylauncher.ui.swing.VersionCellRenderer;
import net.legacylauncher.ui.swing.extended.BorderPanel;
import net.legacylauncher.ui.swing.extended.ExtendedButton;
import net.legacylauncher.ui.swing.extended.ExtendedPanel;
import net.legacylauncher.util.SwingUtil;
import net.minecraft.launcher.updater.VersionSyncInfo;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class VersionList extends CenterPanel implements VersionHandlerListener {
    private static final long serialVersionUID = -7192156096621636270L;
    final VersionHandler handler;
    public final SimpleListModel<VersionSyncInfo> model;
    public final JList<VersionSyncInfo> list;
    final VersionDownloadButton download;
    final VersionRemoveButton remove;
    public final ExtendedButton refresh;
    public final ExtendedButton back;

    VersionList(VersionHandler h) {
        super(squareInsets);
        handler = h;
        BorderPanel panel = new BorderPanel(0, SwingUtil.magnify(5));
        LocalizableLabel label = new LocalizableLabel("version.manager.list");
        panel.setNorth(label);
        model = new SimpleListModel<>();
        list = new JList<>(model);
        list.setCellRenderer(new VersionListCellRenderer(this));
        list.setSelectionMode(2);
        list.addListSelectionListener(e -> handler.onVersionSelected(list.getSelectedValuesList()));
        panel.setCenter(new ScrollPane(list, ScrollPane.ScrollBarPolicy.AS_NEEDED, ScrollPane.ScrollBarPolicy.NEVER));
        ExtendedPanel buttons = new ExtendedPanel(new GridLayout(0, 4));
        refresh = new VersionRefreshButton(this);
        buttons.add(refresh);
        download = new VersionDownloadButton(this);
        buttons.add(download);
        remove = new VersionRemoveButton(this);
        buttons.add(remove);
        back = new ExtendedButton();
        back.setIcon(Images.getIcon24("home"));
        back.addActionListener(e -> handler.exitEditor());
        buttons.add(back);
        panel.setSouth(buttons);
        add(panel);
        handler.addListener(this);
        setSize(SwingUtil.magnify(new Dimension(300, 350)));
    }

    void select(List<VersionSyncInfo> list) {
        if (list != null) {
            int size = list.size();
            int[] indexes = new int[list.size()];

            for (int i = 0; i < size; ++i) {
                indexes[i] = model.indexOf(list.get(i));
            }

            this.list.setSelectedIndices(indexes);
        }
    }

    void deselect() {
        list.clearSelection();
    }

    void refreshFrom(VersionManager manager) {
        setRefresh(false);
        List<VersionSyncInfo> list = manager.getVersions(handler.filter, false);
        model.addAll(list);
    }

    void setRefresh(boolean refresh) {
        model.clear();
        if (refresh) {
            model.add(VersionCellRenderer.LOADING);
        }

    }

    public void block(Object reason) {
        Blocker.blockComponents(reason, list, refresh, remove);
    }

    public void unblock(Object reason) {
        Blocker.unblockComponents(reason, list, refresh, remove);
    }

    public void onVersionRefreshing(VersionManager vm) {
        setRefresh(true);
    }

    public void onVersionRefreshed(VersionManager vm) {
        refreshFrom(vm);
    }

    public void onVersionSelected(List<VersionSyncInfo> version) {
    }

    public void onVersionDeselected() {
    }

    public void onVersionDownload(List<VersionSyncInfo> list) {
        select(list);
    }
}
