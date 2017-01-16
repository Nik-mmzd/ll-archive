package ru.turikhay.tlauncher.ui.notice;

import ru.turikhay.tlauncher.TLauncher;
import ru.turikhay.tlauncher.ui.images.Images;
import ru.turikhay.tlauncher.ui.loc.LocalizableMenuItem;
import ru.turikhay.tlauncher.ui.scenes.DefaultScene;
import ru.turikhay.tlauncher.ui.swing.ResizeableComponent;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainNoticePanel extends NoticePanel implements ResizeableComponent {
    private static int SAFE_ZONE = 10;

    private final DefaultScene defaultScene;

    private final LocalizableMenuItem hideNotice = new LocalizableMenuItem("notice.action.hide");
    {
        hideNotice.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(getNotice() != null) {
                    manager.setHidden(getNotice(), true);
                    manager.selectRandom();
                }
            }
        });
    }

    private final LocalizableMenuItem openNoticeScene = new LocalizableMenuItem("notice.action.scene");
    {
        Images.getScaledIcon("expand.png", 16).setup(openNoticeScene);
        openNoticeScene.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                defaultScene.getMainPane().openNoticeScene();
            }
        });
    }

    public MainNoticePanel(DefaultScene scene) {
        super(scene.getMainPane().getRootFrame().getNotices());
        this.defaultScene = scene;

        popupMenu.registerItem(hideNotice);
        popupMenu.registerItem(openNoticeScene);

        scene.getMainPane().getRootFrame().getNotices().addListener(this, true);
    }

    protected void updateNotice() {
        super.updateNotice();

        if(TLauncher.getInstance() != null && TLauncher.getInstance().isReady()) {
            onResize();
        }
    }

    @Override
    public void onResize() {
        if(getNotice() == null) {
            return;
        }

        Point loginFormLocation = defaultScene.loginForm.getLocation();
        Dimension loginFormSize = defaultScene.loginForm.getSize();

        final int
                fullWidth = getWidth(),
                height = getHeight();

        int x = loginFormLocation.x + loginFormSize.width / 2 - fullWidth / 2;

        if(x + fullWidth > defaultScene.getWidth() - SAFE_ZONE) {
            x = defaultScene.getWidth() - fullWidth - SAFE_ZONE;
        } else if(x < SAFE_ZONE) {
            x = SAFE_ZONE;
        }

        int y;
        switch (defaultScene.getLoginFormDirection()) {
            case TOP_LEFT:
            case TOP:
            case TOP_RIGHT:
            case CENTER_LEFT:
            case CENTER:
            case CENTER_RIGHT:
                y = loginFormLocation.y + loginFormSize.height + SAFE_ZONE;
                break;
            case BOTTOM_LEFT:
            case BOTTOM:
            case BOTTOM_RIGHT:
                y = loginFormLocation.y - height - 10;
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (y + height > defaultScene.getHeight() - SAFE_ZONE) {
            y = defaultScene.getHeight() - height - SAFE_ZONE;
        } else if (y < SAFE_ZONE) {
            y = SAFE_ZONE;
        }

        setLocation(x, y);
    }
}