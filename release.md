# Swing Widgets release history

## v1.1.3, 2026-11-11
- `DatePicker`: the masked text field now declares a minimum size equal to its preferred size. Previously, a GridBagLayout container narrower than the row's preferred width collapsed the picker to just the calendar button, leaving the entered date set but invisible. The fix re-applies automatically when `setLocale()` rebuilds the text field.

## v1.1.2, 2026-07-22
- The date popup now uses the monitor containing the invoking date field, rather than the primary screen, and respects that monitor’s usable bounds.
- `YearMonthPicker`'s popup now respects the configured month range as soon as it opens, instead of briefly offering out-of-range months until you paged the year.
- Fixed `YearMonthPicker` accumulating duplicate popup listeners after repeated open/close cycles, which could fire selection callbacks more than once per click.
- `YearMonthPicker.setSelectedItem(...)` now rejects months outside the configured range, matching `DatePicker`'s behavior.
- `YearMonthPicker` and `YearMonthPickerCombo` no longer throw when constructed with a `null` initial month.
- Fixed a rare crash in `YearMonthPicker` when its calendar icon resource could not be loaded.

## v1.1.1, 2026-05-12
- Changed src/main/java/se/alipsa/datepicker/MaskedDateField.java:336 so editable=false now blocks document replace/remove operations and consumes typed/pressed key events before Backspace, Delete, or
  Arrow handling can mutate text or move the caret.

## v1.1.0, 2026-04-18
Adds a new date picker toolkit for Swing with:

- `DatePicker`: masked text input plus popup calendar
- `CalendarPanel`: standalone month calendar widget
- veto/highlight policies and tooltip support for date cells
- locale-aware formatting, including runtime locale changes

Also improves date entry and picker usability:

- smarter masked input navigation for typing, separators, backspace, and delete
- better year selection in popup calendars
- clearer calendar buttons and more consistent picker sizing

## v1.0.0, 2025-02-21
Initial version
