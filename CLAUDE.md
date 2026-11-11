# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`se.alipsa:swing-widgets` is a small Swing component library with no runtime dependencies. It targets **Java 11** (`maven.compiler.release=11`) and is tested on JDK 11, 17, 21 and 25. Do not use APIs or language features newer than Java 11. JUnit is pinned to 5.x because JUnit 6 needs Java 17, and `version-plugin-rules.xml` stops `versions-maven-plugin` from suggesting 6.x. Running the full build (`mvn verify`) needs **JDK 17+**, because `spotless-maven-plugin` 3.x needs Java 17. The tests themselves still run on JDK 11 (e.g. `mvn package spotbugs:check`, which skips the Spotless check bound to `verify`).

## Commands

```bash
mvn verify                                   # compile, test, spotless:check, spotbugs:check, javadoc
mvn test                                     # tests only
mvn test -Dtest=DatePickerTest               # single test class
mvn test -Dtest=DatePickerTest#testClear     # single test method
mvn spotless:apply                           # reformat (google-java-format 1.23.0, GOOGLE style)
mvn versions:display-plugin-updates versions:display-dependency-updates
```

- `spotless:check` and `spotbugs:check` fail the build at `verify`. Run `mvn spotless:apply` before verifying. Add justified SpotBugs suppressions to `spotbugs-exclude.xml`.
- Javadoc runs with `doclint=all,-missing`, so malformed Javadoc on public API breaks the build.
- Surefire runs with `useModulePath=false`, so tests run on the classpath even though there is a `module-info.java`.
- Manual visual check: `examples/AllControlsDemo.groovy` (`groovy examples/AllControlsDemo.groovy`) shows every widget. It `@Grab`s a *published* version, not the local build. `DatePickerDemo` (in test sources) and the `main` methods in `YearMonthPicker`/`YearMonthPickerCombo` are small launchers.
- Release: `release.sh` keeps the current JDK if it is 17+, otherwise switches to an installed 21, 17 or 25 through SDKMAN (and stops if none is found), then runs `mvn -Prelease clean site deploy` (GPG signing plus Sonatype Central). `relocate.sh`/`relocation-pom.xml` publish the relocation POM for the old `swing-yearmonth-picker` artifact. You normally won't need these.
- Record user-facing changes in `release.md` (release history, newest first). The dependency version in `readme.md` is updated by hand.

## Architecture

There is one JPMS module, **`se.alipsa.symp`** (the historical name, kept for compatibility). It `requires java.desktop` only and exports two packages:

### `se.alipsa.symp` — YearMonth pickers (the original library)
- `YearMonthPickerCombo` extends `JComboBox<YearMonth>`. It is pre-filled with every month in `[from, to]` (by default ±3 years).
- `YearMonthPicker` extends `JPanel`: a label plus a calendar button that opens a `PopupFactory` popup with a `JList` of months and year `<`/`>` paging. The internal list listener and `HoverListCellRenderer` are attached **once** in `initListView()`; user listeners live in a separate list and are fired only for a completed (non-adjusting) pick of a different month, never directly from the `JList`. Range checks go through `isMonthAllowed()`/`monthsInRange()`. The popup is closed by `hidePopup()` (Escape, outside-click `AWTEventListener`, hierarchy changes), which must also remove the AWT listener.

### `se.alipsa.datepicker` — date picker toolkit (added in 1.1.0)
These are three layered components. Each can be used on its own:
- **`MaskedDateField`** (`JTextField`) builds a fixed-width mask from the date pattern. Letters become `_` and separators are fixed. A `DocumentFilter` and key listener enforce the mask, and invalid input reverts on focus loss. It parses through `DateTimeFormatter`, applies the veto policy, and has its own `Consumer<LocalDate>` listeners. `editable=false` must block both document mutations and key handling.
- **`CalendarPanel`** (`JPanel`) is a month grid with month and year navigation popups. It is driven by `DateVetoPolicy` (which disables cells) and `DateHighlightPolicy` (which returns a `HighlightInfo` color and tooltip, or `null`).
- **`DatePicker`** (`JPanel`, `GridBagLayout`) combines a `MaskedDateField` and a calendar `JButton`. Its arrangement comes from `TextFieldPosition`. Key invariants:
  - `lastValidDate` is the source of truth for `getDate()`. Text-field and calendar changes reach it only through `isDateAllowed()` (range plus veto). Listeners fire only when the value actually changes, and a listener that throws is caught and logged so the others still run.
  - The popup is a `JWindow` owned by the parent window. **A fresh `CalendarPanel` is built on every `openPopup()`**, so it always picks up the current locale and policies. The popup is placed with the static, unit-tested `popupLocation(...)` against the *usable bounds of the monitor holding the text field*, not the primary screen. It closes when focus moves to a window it doesn't own.
  - `setLocale()` closes the popup, re-derives the pattern only if `usesDerivedPattern` is set (no explicit pattern was passed to the 5-arg constructor), and then **replaces the `MaskedDateField` instance** through `rebuildTextField()` and `layoutComponents()`. Per-field state therefore has to be re-applied in `rebuildTextField()`: the listener, veto policy, date, enabled flag, and the `minimumSize = preferredSize` fix from 1.1.3. A locale change fires no date event. See `docs/superpowers/specs/2026-04-17-datepicker-mutable-locale-design.md`.
  - Derived patterns come from the locale's `FormatStyle.SHORT` pattern, normalized to fixed width (`yyyy`, `MM`, `dd`) so the mask is stable.

The calendar icon is `src/main/resources/calendar.png`. Both pickers fall back to text (or `null`) when it can't be loaded.

## Tests

The tests are JUnit 5 and cover both packages. They build real Swing components directly, without running on the EDT. Some use reflection to reach private members, such as `CalendarPanel.dateCellValues`/`dateCells`, `DatePicker.onCalendarDateSelected`, `MaskedDateField.revertIfInvalid` and `YearMonthPicker.inputField`. Others call package-private hooks (`DatePicker.derivePattern`, `MaskedDateField.setDateRange`, `YearMonthPicker.monthsInRange`/`popupBaseMonth`/`popupLocation`/`listView`, `HoverListCellRenderer.register`/`setFormatter`). The test sources are compiled into the `se.alipsa.symp` module, so they can only use modules it reads (e.g. not `java.util.logging`). If you rename those members, update the tests too.
