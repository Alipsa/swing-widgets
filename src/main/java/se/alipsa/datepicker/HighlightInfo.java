package se.alipsa.datepicker;

import java.awt.Color;
import java.util.Objects;

public final class HighlightInfo {

    private final Color backgroundColor;
    private final String tooltip;

    public HighlightInfo(Color backgroundColor, String tooltip) {
        this.backgroundColor = backgroundColor;
        this.tooltip = tooltip;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public String getTooltip() {
        return tooltip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HighlightInfo)) return false;
        HighlightInfo that = (HighlightInfo) o;
        return Objects.equals(backgroundColor, that.backgroundColor)
            && Objects.equals(tooltip, that.tooltip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backgroundColor, tooltip);
    }

    @Override
    public String toString() {
        return "HighlightInfo{backgroundColor=" + backgroundColor + ", tooltip='" + tooltip + "'}";
    }
}
