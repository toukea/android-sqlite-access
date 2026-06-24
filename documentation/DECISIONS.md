# Design decisions (ADRs) — android-sqlite-access

Architecture Decision Records for this library. Newest first. See [`../README.md`](../README.md) for
usage and [`README.md`](./README.md) for module orientation.

---

## ADR-0001 — Constructor-based hydration for immutable entities

**Status:** Accepted · **Scope:** `SQLiteModel.asInstance(...)`, new annotations, ProGuard, tests.

### Context
Historically, reading a row into an entity required a **public no-arg constructor** followed by
**field reflection** (`Toolkit.newInstance(clazz)` then `field.set(...)` in
`SQLiteModel.asInstance`). Every persisted entity therefore had to expose mutable, non-`final`
fields and a no-arg constructor — immutable value objects were impossible. `QcmLockHistoryEntry`
(qcmkeystore-app) even documented this constraint in a comment, keeping fields non-`final` purely to
satisfy the ORM.

Key prior finding: the *build* step (`SQLiteModel.fromClass`) and the *cursor read* step
(`DEFAULT_CURSOR_READER`) already work for immutable entities — `final` fields are discovered by
reflection and values are buffered in a map keyed by **column name**. Only `asInstance`
(instantiation + field writes) prevented immutability.

### Decision
Add an alternative **constructor injection** path in `asInstance`, while keeping the legacy field
injection as a 100% backward-compatible fallback.

- **Two parameter → column strategies** (resolved in order, per parameter):
  1. **Annotation** — `@SQLiteModel.Column(name = "…")` on the constructor parameter (extends the
     existing `@Column`, whose `@Target` now includes `PARAMETER`). Obfuscation-proof, API-independent.
  2. **Parameter name** — the parameter's name, when `Parameter.isNamePresent()` is `true` (requires
     compilation with `-parameters`; Android API ≥ 26). Skipped cleanly when unavailable.
- **Constructor selection** — among constructors whose *every* parameter resolves to a known column,
  pick the one covering the **most** columns. Ties prefer a `@SQLiteModel.CreatorConstructor`-annotated
  constructor, then higher arity. Auto-detection means `@CreatorConstructor` is **optional**; it only
  forces/disambiguates and makes a misconfigured annotated constructor fail fast. The chosen
  constructor may be `private` (accessed reflectively). Resolution is memoized per class
  (`CREATOR_PLAN_BUFFER`), mirroring `BUILDER_BUFFER`.
- **Hybrid entities** — columns not covered by the chosen constructor are completed afterwards by
  field injection, but **only on non-`final` fields** (`final` fields are skipped, never written).
- **Fallback & errors** — when no exploitable constructor exists, the exact legacy path runs. If
  neither an exploitable constructor nor a no-arg constructor is available, a clear
  `InstantiationException` is thrown naming the class and the three ways to make it hydratable.
- **New annotations** — `@CreatorConstructor` (constructor marker) and `@Persistable` (type marker
  consumed only by ProGuard). `@Column` gains `PARAMETER` as a target.

### Consequences
- Entities can be immutable: `final` fields, no setters, no no-arg constructor. `QcmLockHistoryEntry`
  was converted as the reference example.
- Existing entities are untouched and keep using field injection.
- Deserialization reuses the pluggable `Serializer` with the resolved `Field`; a `Field`-less
  parameter falls back to a private type coercion (primitive/boxed/String + Gson).
- Verified by a Robolectric suite (annotated, by-name, legacy, hybrid, failure, renamed-column,
  caching) and an instrumented smoke test; consumers (qcmkeystore-app, qcmreader) compile unchanged
  and qcmreader's R8 release succeeds.

### Alternatives considered
- *New `@Param` annotation* instead of extending `@Column` — rejected to avoid two annotations for the
  same concept; `@Column` now targets both fields and parameters.
- *Adding `onDeSerialize(String, Type)` to the public `Serializer`* — rejected as a breaking API
  change; the private `Field`-less coercion covers the rare value-only parameter case.

---

## ADR-0002 — Do not ship `-keepattributes MethodParameters` in consumer rules

**Status:** Accepted · **Scope:** `consumer-rules.pro`.

### Context
The constructor-name strategy needs the `MethodParameters` bytecode attribute to survive R8. The
first draft shipped `-keepattributes MethodParameters` in the library's `consumer-rules.pro`, which is
merged **app-wide** into every consumer. `-keepattributes` cannot be scoped per class, so this would
retain original parameter names for *any* module compiled with `-parameters`, marginally aiding
reverse-engineering of security-sensitive code (e.g. qcmmakerpro's licence/anti-tamper logic).

### Decision
Ship only the annotation attributes (`RuntimeVisibleAnnotations`,
`RuntimeVisibleParameterAnnotations`) needed for the `@Column` strategy. **Do not** ship
`MethodParameters`. Apps that opt into the parameter-name strategy must add both `-parameters` and
`-keepattributes MethodParameters` in their own module (they already need `-parameters` to compile).

### Consequences
- The library imposes **no parameter-name retention** by default; the `@Column` strategy is the
  recommended, obfuscation-proof default.
- None of the shipped rules disable name obfuscation, and **signature-based anti-tamper / licence
  checks are unaffected** (they rely on the APK signature, not on bytecode attributes).
- The trade-off and the per-strategy ProGuard requirements are documented in
  [`../README.md`](../README.md#proguard--r8-consumers).
