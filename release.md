# Swing Widgets release history

## v1.1.3, 2026-11-11
- Demo: preserve native widget borders so the year-month combo is not clipped, and allow scrolling when the demo exceeds the available screen space.
- `DatePicker`: the masked text field now declares a minimum size equal to its preferred size. Previously, a GridBagLayout container narrower than the row's preferred width collapsed the picker to just the calendar button, leaving the entered date set but invisible. The fix re-applies automatically when `setLocale()` rebuilds the text field.
- `DatePicker`: typing a date outside the configured range now marks it invalid and reverts it on focus loss, instead of showing a date that `getDate()` does not return.
- `DatePicker`: an initial date outside the range leaves the picker empty (matching `setDate()`), and a range whose defaulted start falls after its end now throws `IllegalArgumentException`. `clear()` only notifies listeners when a date was set.
- `DatePicker`: the popup closes on Escape and when the picker is hidden or its window moves or resizes; keyboard focus starts in the calendar.
- `MaskedDateField`: quoted literals in patterns are supported, which fixes date entry for Bulgarian locales (`dd.MM.yyyy 'г'.`). Patterns with fields that cannot be typed as fixed-width digits (e.g. `MMM`, single `M`/`d`) now throw `IllegalArgumentException` instead of producing an unusable field.
- `MaskedDateField`: typing over a selection, or pressing Backspace/Delete with a selection, now clears the whole selection; `setText("")` clears the field; a font set with `setFont()` keeps its style; colors come from the look and feel instead of hard-coded black on white.
- `CalendarPanel`: fixed vetoed-date strikethrough sticking to cells after changing month. Colors come from the look and feel. Added keyboard navigation (arrows, Page Up/Down, Home/End, Enter/Space). Typed years in the year popup are applied on Enter.
- `CalendarPanel`: `null` range/initial/locale arguments now use defaults, an initial date outside the range shows the nearest month in range without a selection (previously opening the year popup could throw), `setSelectedDate(null)` clears the selection, and `setSelectedDate()` only notifies listeners on a change.
- `YearMonthPicker`: the popup now formats months with the configured month pattern and locale (it previously showed `yyyy-MM` regardless); `setLocale()` re-renders the value; `setSelectedItem()` updates the displayed text; an out-of-range initial month no longer shows text for an empty value.
- `YearMonthPicker`: year paging can no longer reach months outside the range; listeners are notified once per user pick of a different month (previously also on popup reopen); the popup opens below the button within the screen bounds and closes on Escape, an outside click, or when the window moves.
- `YearMonthPickerCombo`: fixed a `NullPointerException` when painting with a `null` initial month; an out-of-range initial month is no longer selected; `null` range, locale and format arguments use defaults, and `from` after `to` throws `IllegalArgumentException`.
- Listener exceptions are now reported to the thread's uncaught exception handler (with stack trace) instead of printing only the message; other listeners still run.
- Fixed a `NullPointerException` in `HoverListCellRenderer` when hovering over an empty list.
- Build: `mvn test` no longer generates the site.

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
