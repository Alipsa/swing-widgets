#!/usr/bin/env groovy

@Grab('se.alipsa:swing-widgets:1.1.0')

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.time.LocalDate
import java.time.YearMonth
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.DefaultListCellRenderer
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.UIManager
import se.alipsa.datepicker.CalendarPanel
import se.alipsa.datepicker.DatePicker
import se.alipsa.datepicker.HighlightInfo
import se.alipsa.datepicker.MaskedDateField
import se.alipsa.datepicker.TextFieldPosition
import se.alipsa.symp.YearMonthPicker
import se.alipsa.symp.YearMonthPickerCombo

/**
 * Run with:
 *   groovy examples/AllControlsDemo.groovy
 */
void appendLog(JTextArea eventLog, String line) {
  eventLog.append("${line}\n")
  eventLog.caretPosition = eventLog.document.length
}

def titledBorder(String title) {
  BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title)
}

JPanel wrap(String title, Component component) {
  JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6))
  panel.border = titledBorder(title)
  panel.add(component)
  panel
}

DatePicker createDatePicker(
    String name, TextFieldPosition position, Locale locale, JTextArea eventLog) {
  DatePicker picker = new DatePicker(LocalDate.now(), locale)
  picker.textFieldPosition = position
  picker.vetoPolicy = { date -> date.dayOfWeek.value < 6 }
  picker.highlightPolicy = { date ->
    if (date.dayOfMonth == 1) {
      return new HighlightInfo(new Color(220, 242, 255), "First day of month")
    }
    return null
  }
  picker.addListener { date ->
    appendLog(eventLog, "${name} DatePicker -> ${date}")
  }
  picker
}

SwingUtilities.invokeLater {
  try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  } catch (Exception ignored) {
  }

  Locale initialLocale = Locale.US
  JTextArea eventLog = new JTextArea(10, 60)
  eventLog.editable = false

  DatePicker standardPicker =
      createDatePicker("Standard", TextFieldPosition.LEFT, initialLocale, eventLog)
  DatePicker abovePicker =
      createDatePicker("Above", TextFieldPosition.ABOVE, initialLocale, eventLog)
  DatePicker explicitPatternPicker =
      new DatePicker(
          LocalDate.now().minusYears(1),
          LocalDate.now().plusYears(1),
          LocalDate.now(),
          initialLocale,
          "yyyy-MM-dd")
  explicitPatternPicker.addListener { date ->
    appendLog(eventLog, "Explicit pattern DatePicker -> ${date}")
  }

  CalendarPanel calendarPanel = new CalendarPanel(LocalDate.now(), initialLocale)
  calendarPanel.setBorder(titledBorder("CalendarPanel"))
  calendarPanel.setVetoPolicy { date -> date.dayOfWeek.value < 6 }
  calendarPanel.setHighlightPolicy { date ->
    if (date.dayOfMonth == 15) {
      return new HighlightInfo(new Color(255, 244, 179), "Mid-month marker")
    }
    return null
  }
  calendarPanel.addListener { date ->
    appendLog(eventLog, "CalendarPanel -> ${date}")
  }

  MaskedDateField maskedDateField = new MaskedDateField("yyyy-MM-dd", initialLocale)
  maskedDateField.setBorder(titledBorder("MaskedDateField"))
  maskedDateField.setDate(LocalDate.now())
  maskedDateField.setVetoPolicy { date -> date.dayOfWeek.value < 6 }
  maskedDateField.addListener { date ->
    appendLog(eventLog, "MaskedDateField -> ${date}")
  }

  YearMonthPicker yearMonthPicker = new YearMonthPicker(YearMonth.now(), initialLocale)
  yearMonthPicker.setBorder(titledBorder("YearMonthPicker"))
  yearMonthPicker.addListener {
    appendLog(eventLog, "YearMonthPicker -> ${yearMonthPicker.value}")
  }

  YearMonthPickerCombo yearMonthPickerCombo =
      new YearMonthPickerCombo(
          YearMonth.now().minusMonths(6),
          YearMonth.now().plusMonths(6),
          YearMonth.now(),
          initialLocale,
          "MMM yyyy")
  yearMonthPickerCombo.setBorder(titledBorder("YearMonthPickerCombo"))
  yearMonthPickerCombo.addActionListener {
    appendLog(eventLog, "YearMonthPickerCombo -> ${yearMonthPickerCombo.selectedItem}")
  }

  JComboBox<Locale> localeSelector =
      new JComboBox<>(
          [Locale.US, Locale.UK, Locale.GERMANY, new Locale("sv", "SE")] as Locale[])
  localeSelector.renderer =
      new DefaultListCellRenderer() {
        @Override
        Component getListCellRendererComponent(
            JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
          text = value instanceof Locale ? value.displayName : ""
          return this
        }
      }
  localeSelector.selectedItem = initialLocale
  localeSelector.addActionListener {
    Locale locale = localeSelector.selectedItem as Locale
    standardPicker.setLocale(locale)
    abovePicker.setLocale(locale)
    explicitPatternPicker.setLocale(locale)
    appendLog(eventLog, "Locale switched -> ${locale.displayName}")
  }

  JPanel localePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0))
  localePanel.border = titledBorder("Mutable Locale")
  localePanel.add(new JLabel("DatePicker locale:"))
  localePanel.add(localeSelector)
  JButton todayButton = new JButton("Set Today")
  todayButton.addActionListener {
    LocalDate today = LocalDate.now()
    standardPicker.setDate(today)
    abovePicker.setDate(today)
    explicitPatternPicker.setDate(today)
    calendarPanel.setSelectedDate(today)
    maskedDateField.setDate(today)
  }
  localePanel.add(todayButton)

  JPanel formPanel = new JPanel(new GridBagLayout())
  formPanel.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
  GridBagConstraints gbc = new GridBagConstraints()
  gbc.gridx = 0
  gbc.gridy = 0
  gbc.anchor = GridBagConstraints.NORTHWEST
  gbc.insets = new Insets(0, 0, 10, 12)
  gbc.fill = GridBagConstraints.HORIZONTAL
  gbc.weightx = 1.0
  formPanel.add(localePanel, gbc)

  gbc.gridy++
  formPanel.add(wrap("DatePicker (Left)", standardPicker), gbc)

  gbc.gridy++
  formPanel.add(wrap("DatePicker (Above)", abovePicker), gbc)

  gbc.gridy++
  formPanel.add(wrap("DatePicker (Explicit Pattern)", explicitPatternPicker), gbc)

  gbc.gridy++
  formPanel.add(wrap("MaskedDateField", maskedDateField), gbc)

  gbc.gridy++
  formPanel.add(wrap("YearMonthPicker", yearMonthPicker), gbc)

  gbc.gridy++
  formPanel.add(wrap("YearMonthPickerCombo", yearMonthPickerCombo), gbc)

  JPanel rightPanel = new JPanel()
  rightPanel.layout = new BoxLayout(rightPanel, BoxLayout.Y_AXIS)
  rightPanel.border = BorderFactory.createEmptyBorder(12, 0, 12, 12)
  rightPanel.add(calendarPanel)
  rightPanel.add(Box.createVerticalStrut(12))
  rightPanel.add(new JScrollPane(eventLog))

  JPanel content = new JPanel(new BorderLayout())
  content.add(formPanel, BorderLayout.CENTER)
  content.add(rightPanel, BorderLayout.EAST)

  JFrame frame = new JFrame("Swing Widgets Demo")
  frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
  frame.contentPane = content
  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.visible = true
}
