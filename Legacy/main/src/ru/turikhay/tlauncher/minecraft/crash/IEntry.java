package ru.turikhay.tlauncher.minecraft.crash;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import ru.turikhay.util.StringUtil;
import ru.turikhay.util.U;

public class IEntry {
    private final CrashManager manager;
    private final String name;

    public IEntry(CrashManager manager, String name) {
        this.manager = U.requireNotNull(manager);
        this.name = StringUtil.requireNotBlank(name, "name");
        logName = "[" + getClass().getSimpleName() + ":" + name + "]";
    }

    public final CrashManager getManager() {
        return manager;
    }

    public final String getName() {
        return name;
    }

    public final String toString() {
        return buildToString().build();
    }

    protected ToStringBuilder buildToString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", getName());
    }

    private final String logName;

    protected final void log(Object... o) {
        getManager().log(logName, o);
    }
}
