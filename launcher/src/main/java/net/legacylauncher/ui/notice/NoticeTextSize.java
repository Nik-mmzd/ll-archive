package net.legacylauncher.ui.notice;

import net.legacylauncher.util.SwingUtil;

import java.awt.*;
import java.util.Objects;

class NoticeTextSize {
    private final Notice notice;

    NoticeTextSize(Notice notice) {
        this.notice = Objects.requireNonNull(notice, "notice");
    }

    Notice getNotice() {
        return notice;
    }

    Dimension get(ParamPair param) {
        return SwingUtil.waitAndReturn(() -> new SizeCalculator(this, param).get());
    }
}
