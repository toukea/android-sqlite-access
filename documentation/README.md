# android-sqlite-access — module documentation

Orientation index for the `android-sqlite-access` library (published artifact
`istat.android.data.access.sqlite:istat-access-sqlite`). This is the home-grown SQLite ORM + fluent
query builder used across the QmakerTech Android apps (qcmreader, qcmmaker, qcmmakerpro, qcmkeystore-app…)
through the `:android-processor-tools` → `:qcmmakercore` dependency chain.

## Where to look

| You want… | Go to |
|---|---|
| Usage, API examples, query DSL, hydration strategies, ProGuard | [`../README.md`](../README.md) (library README) |
| Design decisions / rationale (ADRs) | [`DECISIONS.md`](./DECISIONS.md) |
| Workspace-wide documentation rules | [`../../DOC_STRATEGY.md`](../../DOC_STRATEGY.md) |
| Cross-module / architecture docs | [`../../documents-qmaker-private/`](../../documents-qmaker-private/) (`shared/architecture/android/`) |

## Quick facts

- **Type:** Android library (`com.android.library`), Java 17, `minSdk 26`, `compileSdk 34`.
- **Mapping:** POJO ↔ table by class/field names; annotations are optional (see the
  [annotations reference](../README.md#annotations-reference)).
- **Hydration:** field injection (legacy) **and** constructor injection for immutable entities — see
  [ADR-0001](./DECISIONS.md#adr-0001--constructor-based-hydration-for-immutable-entities).
- **Tests:** Robolectric unit tests in `src/test`, instrumented tests in `src/androidTest`.

> Documentation language for this module is **English** (technical/private), per `DOC_STRATEGY.md`.
