# Swing Widgets
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa/swing-widgets/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa/swing-widgets)
[![javadoc](https://javadoc.io/badge2/se.alipsa/swing-widgets/javadoc.svg)](https://javadoc.io/doc/se.alipsa/swing-widgets)

Useful Swing components for java 11 or higher (tested with jdk 11 and 17).

## YearMonth Picker
A year-month date picker component for java swing.

There are two versions:
1. YearMonthPickerCombo - a combobox that return a YearMonth.
2. YearMonthPicker - A custom control that resembles DatePicker where the user can click a calendar and then pick the
   desired year month.

Both works similar to a combo box i.e. you do getValue() to get the value and
setOnAction() to capture a value change e.g.
```java
import se.alipsa.symp.YearMonthPicker;

class Example {
  public static void main(String[] args) {
    JFrame frame = new JFrame("Year month combo");
    JPanel panel = new JPanel();
    frame.add(panel);
    YearMonthPicker ymp = new YearMonthPicker();
    ymp.addListener(e -> System.out.println(ymp.getValue()));
    panel.add(new JLabel("Default Combo: "));
    panel.add(ymp);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
```

There are no external dependencies, and the jar file is about 40kb so nice and small. You can download the jar file from
the release section or add the following to your maven file:
```xml
<dependency>
  <groupId>se.alipsa</groupId>
  <artifactId>swing-widgets</artifactId>
  <version>1.0.0</version>
</dependency>
```
...or the equivalent to you favorite build tool.
The module name for this library is `se.alipsa.symp`

## se.alipsa.symp.YearMonthPickerCombo
<img src="https://raw.githubusercontent.com/Alipsa/swing-widgets/master/docs/YearMonthPickerCombo.png" alt="YearMonthPickerCombo Screenshot" width="350" />
There are 5 constructors:

__YearMonthPickerCombo()__
This gives you YearMonths 3 year back and 3 years into the future from now.

__YearMonthPickerCombo(YearMonth initial)__
This gives you YearMonths 3 year back and 3 years into the future from initial.

__YearMonthPickerCombo(YearMonth from, YearMonth to, YearMonth initial)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected.

__YearMonthPickerCombo(YearMonth from, YearMonth to, YearMonth initial, Locale locale)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected displayed in the locale specified formatted as "yyyy-MM".

__YearMonthPickerCombo(YearMonth from, YearMonth to, YearMonth initial, Locale locale, String format)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected, displayed in the locale specified in the format specified.

## se.alipsa.symp.YearMonthPicker
<img src="https://raw.githubusercontent.com/Alipsa/swing-widgets/master/docs/YearMonthPicker.png" alt="YearMonthPickerCombo Screenshot" width="350" />
There are 5 constructors:

__YearMonthPicker()__ This gives you YearMonths 3 year back and 3 years into the future from now in
the system default locale.

__YearMonthPicker(YearMonth initial)__
This gives you YearMonths 3 year back and 3 years into the future from initial in
the system default locale.

__YearMonthPicker(YearMonth initial, Locale locale)__
This gives you YearMonths 3 year back and 3 years into the future from initial in
the specified locale. Month names are in full (long) format.

__YearMonthPicker(YearMonth from, YearMonth to, YearMonth initial)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected in the default locale.
The display value (when a year month is chosen) will be displayed in the
format "yyyy-MM".

__YearMonthPicker(YearMonth from, YearMonth to, YearMonth initial, String monthNameFormat)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected in the default locale. MonthNameFormat is how the
month names will be displayed in the popup. Set it to "MMM" for the letter short style
or include the year with "yyyy-MMM" or whatever.
The display value (when a year month is chosen) will be displayed in the
format "yyyy-MM".

__YearMonthPicker(YearMonth from, YearMonth to, YearMonth initial, Locale locale, String monthNameFormat)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected in the locale specified. MonthNameFormat is how the
month names will be displayed in the popup. Default is "MMMM" (long format), set to "MMM" for the letter short style
or include the year with "yyyy-MMM" or whatever. The display value (when a year month is chosen) will be displayed in the
format "yyyy-MM".

__YearMonthPicker(YearMonth from, YearMonth to, YearMonth initial, Locale locale, String monthPattern, String yearMonthPattern)__
This gives you all yearmonths between from and to (both from and to included) with the
initial value as the default selected in the locale specified. MonthNameFormat is how the
month names will be displayed in the popup. Default is "MMMM" (long format), set to "MMM" for the letter short style
or include the year with "yyyy-MMM" or whatever. The display value (when a year month is chosen) will be displayed in the
format provided with the yearMonthPattern argument.

## DatePicker

Package: `se.alipsa.datepicker`

A date picker component with a masked text field and calendar popup.

### Basic usage

```java
import se.alipsa.datepicker.DatePicker;

class Example {
  public static void main(String[] args) {
    JFrame frame = new JFrame("Date picker");
    JPanel panel = new JPanel();
    frame.add(panel);
    DatePicker dp = new DatePicker();
    dp.addListener(date -> System.out.println(date));
    panel.add(new JLabel("Pick a date: "));
    panel.add(dp);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
```

### Constructors

__DatePicker()__
Uses today as initial date, system locale, locale-derived date pattern, range of +/- 20 years.

__DatePicker(LocalDate initial)__
Uses the specified initial date, system locale, locale-derived date pattern, range of +/- 20 years from initial.

__DatePicker(LocalDate initial, Locale locale)__
Uses the specified initial date and locale, locale-derived date pattern, range of +/- 20 years from initial.

__DatePicker(LocalDate from, LocalDate to, LocalDate initial)__
Custom date range with system locale and locale-derived date pattern.

__DatePicker(LocalDate from, LocalDate to, LocalDate initial, Locale locale)__
Custom date range and locale with locale-derived date pattern.

__DatePicker(LocalDate from, LocalDate to, LocalDate initial, Locale locale, String datePattern)__
Full control over range, locale, and date pattern (e.g. "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy").

### Veto and Highlight Policies

```java
// Disallow weekends
dp.setVetoPolicy(date -> date.getDayOfWeek().getValue() < 6);

// Highlight holidays
dp.setHighlightPolicy(date -> {
    if (date.getMonthValue() == 12 && date.getDayOfMonth() == 25) {
        return new HighlightInfo(Color.RED, "Christmas");
    }
    return null;
});
```

### CalendarPanel (standalone)

The CalendarPanel can be used independently without the text field and popup:

```java
import se.alipsa.datepicker.CalendarPanel;

CalendarPanel calendar = new CalendarPanel();
calendar.addListener(date -> System.out.println(date));
panel.add(calendar);
```