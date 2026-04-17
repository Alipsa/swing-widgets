package se.alipsa.symp;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.YearMonth;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/** List cell renderer that highlights the list item under the mouse cursor. */
public class HoverListCellRenderer extends DefaultListCellRenderer {

  private static final Color HOVER_COLOR = Color.LIGHT_GRAY;
  private int hoverIndex = -1;

  /** Creates a hover-aware list cell renderer. */
  public HoverListCellRenderer() {}

  static void register(JList<YearMonth> l) {
    HoverListCellRenderer renderer = new HoverListCellRenderer();
    l.setCellRenderer(renderer);
    l.addMouseListener(renderer.getHandler(l));
    l.addMouseMotionListener(renderer.getHandler(l));
  }

  /**
   * Returns the component used to render a list cell with hover-aware background styling.
   *
   * @param list the list being rendered
   * @param value the cell value to render
   * @param index the list index being rendered
   * @param isSelected whether the cell is selected
   * @param cellHasFocus whether the cell currently has focus
   * @return the configured renderer component
   */
  @Override
  public Component getListCellRendererComponent(
      JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

    if (!isSelected) {
      setBackground(index == hoverIndex ? HOVER_COLOR : list.getBackground());
    }
    return this;
  }

  /**
   * Returns the mouse handler used to track hover state for the list.
   *
   * @param list the list to monitor
   * @return a mouse handler for the list
   */
  public MouseAdapter getHandler(JList<YearMonth> list) {
    return new HoverMouseHandler(list);
  }

  class HoverMouseHandler extends MouseAdapter {

    private final JList<YearMonth> list;

    /**
     * Creates a handler that updates hover state for the supplied list.
     *
     * @param list the list whose hover state should be tracked
     */
    public HoverMouseHandler(JList<YearMonth> list) {
      this.list = list;
    }

    /**
     * Clears the hover state when the mouse leaves the list.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseExited(MouseEvent e) {
      setHoverIndex(-1);
    }

    /**
     * Updates the hover state to match the item currently under the cursor.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
      int index = list.locationToIndex(e.getPoint());
      setHoverIndex(list.getCellBounds(index, index).contains(e.getPoint()) ? index : -1);
    }

    private void setHoverIndex(int index) {
      if (hoverIndex == index) {
        return;
      }
      hoverIndex = index;
      list.repaint();
    }
  }
}
