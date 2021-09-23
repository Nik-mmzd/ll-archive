package ru.turikhay.tlauncher.ui;

import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.managers.ConnectivityManager;
import ru.turikhay.tlauncher.ui.images.Images;
import ru.turikhay.tlauncher.ui.loc.Localizable;
import ru.turikhay.tlauncher.ui.loc.LocalizableComponent;
import ru.turikhay.tlauncher.ui.loc.LocalizableHTMLLabel;
import ru.turikhay.tlauncher.ui.loc.LocalizableLabel;
import ru.turikhay.tlauncher.ui.swing.ScrollPane;
import ru.turikhay.tlauncher.ui.swing.editor.EditorPane;
import ru.turikhay.tlauncher.ui.swing.extended.BorderPanel;
import ru.turikhay.tlauncher.ui.swing.extended.ExtendedFrame;
import ru.turikhay.tlauncher.ui.swing.extended.ExtendedLabel;
import ru.turikhay.tlauncher.ui.swing.extended.ExtendedPanel;
import ru.turikhay.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ConnectivityWarning extends ExtendedFrame implements LocalizableComponent {
    private final static int WIDTH = SwingUtil.magnify(500);
    private static final int BORDER = SwingUtil.magnify(20);
    private final static int WIDTH_BORDERED = WIDTH - 2*BORDER;
    private static final int HALF_BORDER = BORDER / 2;

    private final EditorPane body;
    private final ExtendedPanel entriesPanel;

    private boolean noConnection;

    public ConnectivityWarning() {
        setIconImages(SwingUtil.createFaviconList("warning"));
        setMaximumSize(new Dimension(WIDTH, Integer.MAX_VALUE));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //setResizable(false);

        ExtendedPanel p = new ExtendedPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(
                BORDER,
                BORDER,
                BORDER,
                BORDER
        ));
        setContentPane(p);

        LocalizableLabel title = new LocalizableLabel("connectivity.warning.title");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 3.f));
        title.setIconTextGap(HALF_BORDER);
        title.setIcon(Images.getIcon24("plug-1"));
        add(title);
        add(Box.createRigidArea(new Dimension(1, BORDER)));

        body = new EditorPane();
        body.setContentType("text/html");
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setAlignmentY(Component.TOP_ALIGNMENT);
        add(body);
        add(Box.createRigidArea(new Dimension(1, BORDER)));

        entriesPanel = new ExtendedPanel();
        entriesPanel.setLayout(new GridBagLayout());
        entriesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        entriesPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        ScrollPane scrollPane = new ScrollPane(entriesPanel);
        scrollPane.getViewport().setAlignmentY(Component.TOP_ALIGNMENT);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
        scrollPane.setHBPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVBPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        add(scrollPane);

        updateLocale();
    }

    public void updateEntries(List<ConnectivityManager.Entry> entries) {
        entriesPanel.removeAll();

        noConnection = entries.stream().allMatch(e -> e.isDone() && !e.isReachable());
        List<ConnectivityManager.Entry> unreachableEntries = entries.stream()
                .filter(e -> !e.isReachable())
                .sorted(
                        Comparator.comparing(ConnectivityManager.Entry::isDone, Boolean::compareTo)
                                .reversed()
                                .thenComparing(
                                        Comparator.comparing(ConnectivityManager.Entry::getPriority).reversed()
                                )
                                .thenComparing(ConnectivityManager.Entry::getName)
                )
                .collect(Collectors.toList());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        if(noConnection || unreachableEntries.isEmpty()) {
            updateLocale();
        } else {
            c.gridy++;
            entriesPanel.add(new JSeparator(), c);
            c.gridy++;
            entriesPanel.add(Box.createRigidArea(new Dimension(1, HALF_BORDER)), c);
            boolean officialRepoUnavailable = unreachableEntries.stream()
                    .filter(ConnectivityManager.Entry::isDone)
                    .anyMatch(e -> e.getName().equals("official_repo"));
            boolean officialRepoMirrorUnavailable = unreachableEntries.stream()
                    .filter(ConnectivityManager.Entry::isDone)
                    .anyMatch(e -> e.getName().equals("official_repo_proxy"));
            for (ConnectivityManager.Entry entry : unreachableEntries) {
                if (entry.isReachable()) {
                    continue;
                }
                BorderPanel panel = new BorderPanel();
                panel.setVgap(SwingUtil.magnify(HALF_BORDER));
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.setAlignmentY(Component.TOP_ALIGNMENT);
                ExtendedLabel name;
                if (entry.getName().startsWith("repo_")) {
                    name = new LocalizableLabel(
                            "connectivity.warning.list.name.repo",
                            entry.getName().substring("repo_".length())
                    );
                } else {
                    String path = "connectivity.warning.list.name." + entry.getName();
                    if (Localizable.nget(path) != null) {
                        name = new LocalizableLabel(path);
                    } else {
                        name = new LocalizableLabel("connectivity.warning.list.name.web", entry.getName());
                    }
                }
                if (entry.isDone()) {
                    name.setFont(name.getFont().deriveFont(Font.BOLD));

                    String path;
                    if (entry.getName().equals("official_repo")) {
                        path = "connectivity.warning.list.hint.official_repo." +
                                (officialRepoMirrorUnavailable ? "not_ok" : "ok");
                    } else if (entry.getName().equals("official_repo_proxy")) {
                        path = "connectivity.warning.list.hint.official_repo_proxy." +
                                (officialRepoUnavailable ? "not_ok" : "ok");
                    } else {
                        path = "connectivity.warning.list.hint." + entry.getName();
                    }
                    if (Localizable.nget(path) != null) {
                        LocalizableHTMLLabel hint = new LocalizableHTMLLabel(path);
                        hint.setLabelWidth(WIDTH_BORDERED);
                        panel.setSouth(hint);
                    }
                }
                panel.setWest(name);
                LocalizableLabel status = new LocalizableLabel(
                        "connectivity.warning.list.status." + (entry.isDone() ? "unreachable" : "waiting"));
                if (entry.isDone()) {
                    status.setIcon(Images.getIcon16("warning"));
                }
                panel.setEast(status);
                c.gridy++;
                entriesPanel.add(panel, c);
                c.gridy++;
                entriesPanel.add(Box.createRigidArea(new Dimension(1, HALF_BORDER)), c);
                JSeparator s = new JSeparator();
                s.setAlignmentX(Component.LEFT_ALIGNMENT);
                s.setAlignmentY(Component.TOP_ALIGNMENT);
                c.gridy++;
                entriesPanel.add(s, c);
                c.gridy++;
                entriesPanel.add(Box.createRigidArea(new Dimension(1, HALF_BORDER)), c);
            }
            // make the contents stick to the top
            c.weighty = 1.0;
            c.gridy++;
            entriesPanel.add(Box.createRigidArea(new Dimension(1, 1)), c);
        }
        SwingUtil.later(() -> {
            revalidate();
            repaint();
        });
    }

    @Override
    public void updateLocale() {
        setTitle(Localizable.get("connectivity.warning.title"));
        if(noConnection) {
            body.setText(String.format(Locale.ROOT, "%s <a href=\"%s\">%s</a>",
                    Localizable.get("connectivity.warning.body.empty"),
                    TLauncher.getInstance().getSettings().isUSSRLocale() ?
                            "https://tlaun.ch/support/noconnectivity/ru" : "https://tlaun.ch/support/noconnectivity",
                    Localizable.get("connectivity.warning.body.link")
            ));
        } else {
            body.setText(String.format(Locale.ROOT, "%s <a href=\"%s\">%s</a>",
                    Localizable.get("connectivity.warning.body.text"),
                    TLauncher.getInstance().getSettings().isUSSRLocale() ?
                            "https://tlaun.ch/support/connectivity/ru" : "https://tlaun.ch/support/connectivity",
                    Localizable.get("connectivity.warning.body.link")
            ));
        }
        body.setPreferredSize(new Dimension(WIDTH_BORDERED, SwingUtil.getPrefHeight(body, WIDTH_BORDERED)));
        body.setMaximumSize(new Dimension(WIDTH_BORDERED, SwingUtil.getPrefHeight(body, WIDTH_BORDERED)));
    }
}