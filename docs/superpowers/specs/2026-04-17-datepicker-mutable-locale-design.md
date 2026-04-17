# DatePicker Mutable Locale — Design Spec

Date: 2026-04-17

## Goal

Allow the locale of a `DatePicker` to be changed at runtime without recreating the component. The parent layout must not be affected.

## Scope

Changes are confined to `DatePicker.java` and `DatePickerTest.java`. `MaskedDateField` and `CalendarPanel` are unchanged.

## State Changes in DatePicker

| Field | Before | After |
|-------|--------|-------|
| `locale` | `final` | mutable |
| `datePattern` | `final` | mutable |
| `usesDerivedPattern` | (new) | `boolean` — `true` when pattern was locale-derived |

`usesDerivedPattern` is `false` only when the 6-arg constructor receives a non-null `datePattern` argument. All other constructors set it to `true`. This lets the picker distinguish "the caller explicitly chose `yyyy-MM-dd`" from "we happened to derive `yyyy-MM-dd` from the locale" even though both cases produce the same resolved string.

## setLocale(Locale) Behaviour

```
1. if newLocale is null → return (ignored)
2. if newLocale equals current locale → return (no-op)
3. capture: date = getDate(), enabled = isEnabled()
4. closePopup()                         // disposes popup, nulls calendarPanel
5. this.locale = newLocale
6. if usesDerivedPattern → this.datePattern = derivePattern(newLocale)
7. rebuild textField:
     a. new MaskedDateField(datePattern, locale)
     b. addListener(this::onTextFieldDateChanged)
     c. setVetoPolicy(vetoPolicy)
     d. if date != null → textField.setDate(date)   // via getDate()/setDate(), not raw text
     e. textField.setEnabled(enabled)
8. layoutComponents()                   // removeAll() + re-add with current TextFieldPosition
```

`lastValidDate`, `vetoPolicy`, `highlightPolicy`, `textFieldPosition`, and enabled state are all preserved. No listener event is fired — locale change is not a date change.

Date round-trip uses `getDate()` / `setDate(LocalDate)`, not `getText()` — this avoids carrying over partially typed invalid input across locale changes.

`CalendarPanel` needs no changes. `openPopup()` already constructs it fresh each time, so it picks up the updated `locale` and `datePattern` automatically.

## Explicit vs Derived Pattern

| Construction path | `usesDerivedPattern` | On locale change |
|-------------------|----------------------|------------------|
| `new DatePicker()` | `true` | pattern re-derived from new locale |
| `new DatePicker(initial)` | `true` | pattern re-derived from new locale |
| `new DatePicker(from, to, initial, locale)` | `true` | pattern re-derived from new locale |
| `new DatePicker(from, to, initial, locale, "yyyy-MM-dd")` | `false` | pattern unchanged |

## Stale Reference Risk

`getTextField()` returns the current `MaskedDateField`. After `setLocale()` the field is replaced. Any caller that cached the result of `getTextField()` before calling `setLocale()` holds a stale reference. This is documented behaviour — callers must not cache the returned field across locale changes.

## Tests to Add (DatePickerTest)

1. `testSetLocaleDerivedPatternUpdates` — verify pattern changes when locale changes on a locale-constructed picker
2. `testSetLocaleKeepsExplicitPattern` — verify pattern is unchanged for a pattern-constructed picker
3. `testSetLocalePreservesDate` — verify `getDate()` returns same date after locale change
4. `testSetLocalePreservesDisabledState` — verify a disabled picker stays disabled after locale change
5. `testSetLocalePreservesVetoPolicy` — verify veto policy still blocks after locale change
6. `testSetLocaleSameLocaleIsNoOp` — verify calling with same locale does not replace textField instance
7. `testSetLocaleNullIsIgnored` — verify null locale does not throw and does not change state
