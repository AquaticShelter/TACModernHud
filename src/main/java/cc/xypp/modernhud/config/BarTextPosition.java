package cc.xypp.modernhud.config;

/** Where a bar's number readout (HP or Armor) is drawn relative to the rest of the widget. */
public enum BarTextPosition {
    /** A new line directly under the bar it belongs to. */
    BELOW_BAR,
    /** A new line directly under the head/name row. */
    BELOW_NAME_HEAD,
    /** Right after the bar's right edge, on the same line. */
    END_OF_BAR
}
