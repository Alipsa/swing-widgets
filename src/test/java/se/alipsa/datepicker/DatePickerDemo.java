package se.alipsa.datepicker;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class DatePickerDemo {

    private DatePickerDemo() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("DatePicker Demo");
            JPanel panel = new JPanel();
            frame.add(panel);

            DatePicker picker = new DatePicker();
            picker.addListener(date -> System.out.println("Selected: " + date));
            panel.add(new JLabel("Date: "));
            panel.add(picker);

            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
