# Swing Widgets release history

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
