<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: UI Styling Updates

- **Plan**: context/changes/ui-styling-updates/plan.md
- **Scope**: All 6 phases (comprehensive review)
- **Date**: 2026-09-02
- **Verdict**: REJECTED
- **Findings**: 4 critical, 3 warnings, 0 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | FAIL ❌ |
| Scope Discipline | FAIL ❌ |
| Safety & Quality | PASS ✅ |
| Architecture | FAIL ❌ |
| Pattern Consistency | PASS ✅ |
| Success Criteria | FAIL ❌ |

## Findings

### F1 — Complete Color Palette Divergence

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Plan Adherence
- **Location**: frontend/tailwind.config.js, frontend/src/index.css
- **Detail**: Plan specified warm 1970s retro palette (cream #E8E2D4, green #436741, rose #E1C1C6) with multi-color harmony. Implementation uses completely different palette (canvas #222222 dark gray, accent #C2D8C4 soft sage). Colors used across all 20+ modified files, affecting visual consistency, component behavior, and entire brand direction. This is not a color tweak—it's a fundamental aesthetic pivot.
- **Fix A ⭐ Recommended**: Restore to approved 1970s palette and document any stakeholder request for redesign as a separate change
  - Strength: Honors the completed plan; if new design is desired, it gets proper review via a new change request rather than silently diverging from approved work.
  - Tradeoff: If the minimalist direction is intentional, requires reverting substantial work.
  - Confidence: HIGH — plan is documented and stakeholders approved it; any pivot should be explicit.
  - Blind spot: Haven't checked whether product team requested the new palette offline.
- **Fix B**: Accept the new palette as superior and document the decision
  - Strength: New palette (dark gray + sage) is cohesive and rendered safely (code review passed).
  - Tradeoff: Invalidates the entire approved plan without recorded decision; sets precedent that post-approval redesigns don't require stakeholder sign-off.
  - Confidence: LOW — this is a governance issue, not a code quality issue.
  - Blind spot: Stakeholder awareness and approval for the pivot.
- **Decision**: ACCEPTED — Canvas + Accent palette intentionally replaces 1970s retro. Design pivot approved; will document as architectural revision in change record.

### F2 — Light-Only Scope Requirement Violated

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Scope Discipline
- **Location**: frontend/src/index.css:34-37
- **Detail**: Plan explicitly states "Light-only design (user requested this)" as a scope guardrail. Current implementation sets `color-scheme: dark` and applies dark background (#818243). This violates a fundamental user requirement captured in the plan's "What We're NOT Doing" section. Either the requirement changed and wasn't documented, or this is an unintended drift.
- **Fix**: Restore light-only design by removing `color-scheme: dark` and rebalancing the palette for light backgrounds
  - Strength: Restores adherence to documented scope.
  - Tradeoff: May require significant palette adjustments if the new olive/mauve combo was chosen specifically for dark backgrounds.
  - Confidence: MEDIUM — depends on whether the color scheme change was intentional or coincidental.
  - Blind spot: Haven't verified whether end-users prefer dark mode despite the original request.
- **Decision**: SKIPPED — Deferred. Need to verify with stakeholder whether light-only constraint still applies or if dark-first design is the new intentional requirement.

### F3 — Missing Retro Shadows (Central Design Feature)

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Plan Adherence
- **Location**: frontend/tailwind.config.js (no boxShadow extensions), frontend/src/index.css
- **Detail**: Plan explicitly defines retro shadows as a key visual feature: soft offset shadows (4-6px) with low opacity (8-18%) to create "tactile retro feel" and "authentic 1970s" aesthetic. The plan discovery states "soft offset shadows (2-6px) with low opacity (8-18%) rather than modern blur-based shadows." Current implementation removes all shadows entirely and relies only on opacity layers. This eliminates a central design pillar.
- **Fix**: Restore retro shadow utilities to Tailwind config
  - Strength: Restores the tactile, warm aesthetic that differentiates the design.
  - Tradeoff: Adds back 4-6 lines to Tailwind config; if the minimalist direction was intentional, shadows may not fit the new vision.
  - Confidence: MEDIUM — shadow removal could be intentional, but if so, it should have been recorded as a design decision.
  - Blind spot: Haven't confirmed whether shadows are intentionally omitted for the new aesthetic direction.
- **Decision**: SKIPPED — Minimalist direction (no shadows, opacity-only) is intentional. Shadows omitted by design.

### F4 — Button Styling Completely Divergent from Plan

- **Severity**: ❌ CRITICAL
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Plan Adherence
- **Location**: frontend/src/shared/components/Button.tsx:21-26
- **Detail**: Plan specifies button variants with solid fills and color darkening on hover (primary: green-600/700/800, secondary: rose-100/200, danger: rose-100/200/300 with darkening via HSL lightness reduction). Implementation uses transparent backgrounds with hairline borders and opacity shifts (primary: `bg-accent text-canvas border border-accent`, secondary: `bg-transparent border border-accent/50`, danger: `bg-transparent border border-accent/70`). This is a different interaction paradigm—from filled solid buttons to outline/hairline buttons—affecting visual hierarchy, accessibility, and user perception across all interactive elements.
- **Fix**: Restore button variants to match plan (filled primary, optional outlined secondary/danger)
  - Strength: Aligns with approved design; uses color darkening pattern documented in plan for reliable hover states.
  - Tradeoff: If outline pattern was intentional, requires reverting this change and re-approving.
  - Confidence: HIGH — button patterns are well-documented in the plan with specific color values.
  - Blind spot: None significant.
- **Decision**: SKIPPED — Outline buttons (hairline) are the new interaction paradigm for the minimalist direction. Kept intentionally.

### W1 — Border Radius Values Changed from Authentic 6px to Modern Curves

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Plan Adherence
- **Location**: frontend/tailwind.config.js:32-40
- **Detail**: Plan explicitly discovers and documents that "6px rounded corners feel more authentically 1970s" and specifies avoiding `rounded-lg` (16px) as "too modern." Current implementation defines `pill: 9999px` and `card: 1.75rem (28px)`, contradicting the plan's aesthetic direction. The pill radius in particular (fully circular) is maximally modern, not retro.
- **Fix A ⭐ Recommended**: Update border radius values to match plan (6px standard, avoid 28px and 9999px)
  - Strength: Restores the retro aesthetic rationale documented in plan discoveries.
  - Tradeoff: Changes multiple component corners; if the modern curves were intentional, breaks that design.
  - Confidence: HIGH — this is an explicit plan discovery.
  - Blind spot: Haven't verified whether the new corners are intentional or accidental.
- **Fix B**: Accept modern border radius as better UX and document the decision
  - Strength: Modern curves may provide better visual polish than aggressive 6px.
  - Tradeoff: Silently deviates from plan discovery without recording the decision.
  - Confidence: LOW — this should be an explicit design choice, not a drift.
  - Blind spot: Design rationale for the change.
- **Decision**: ACCEPTED — Modern border radius (28px cards, 9999px pills) intentionally replaces 6px spec. Documented as design pivot.

### W2 — Additional Typography (Rammetto One) Not in Plan Scope

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Scope Discipline
- **Location**: frontend/src/index.css:1-22, frontend/tailwind.config.js
- **Detail**: Plan specifies "Playfair Display serif for h1-h3, body text remains sans-serif." Implementation adds self-hosted Rammetto One as a brand/wordmark font. This is an out-of-scope addition that increases font loading complexity (now 3 font families: Rammetto One, Playfair Display, Inter). The addition is not documented in plan phases and represents scope creep.
- **Fix**: Remove Rammetto One; document any brand typography as a separate change request if needed
  - Strength: Keeps scope boundaries clean; if the wordmark font is truly needed, it gets proper review via a new change.
  - Tradeoff: Loses the branding benefit if Rammetto One was intentional.
  - Confidence: HIGH — the plan is explicit about what typography is in scope.
  - Blind spot: Whether Rammetto One is critical for brand consistency.
- **Decision**: SKIPPED — Rammetto One is essential for brand identity. Kept as part of design system.

### W3 — Theme Colors Replaced Instead of Extended (Architecture Tradeoff)

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Architecture
- **Location**: frontend/tailwind.config.js:16-22
- **Detail**: Plan specifies using `theme.extend.colors` to "preserve all default Tailwind colors while adding custom ones." Implementation **replaces** the entire color palette using `theme.colors` (removing all default blue/gray/red Tailwind utilities). While this is intentional and safe (verified by code review as "Excellent design"), it's an architectural choice not documented in the plan. The tradeoff: stricter palette enforcement (safer) vs. less flexibility (can't use standard Tailwind colors as fallback).
- **Fix**: This is already working correctly; decision is whether to document this as intentional or revert to `extend`
  - Strength: Current approach (replace) is safer; prevents off-palette utilities from silently entering the design.
  - Tradeoff: Removes flexibility; if a standard color is ever needed, requires adding it explicitly.
  - Confidence: HIGH — code review confirms this is a deliberate safety feature.
  - Blind spot: Whether the stricter constraint is acceptable long-term.
- **Decision**: ACCEPTED — Color palette replacement approach is intentional safety feature. Stricter constraint is acceptable and preferred.

---

## Summary

**Four critical findings** all point to the same root cause: **The implementation does not match the approved plan across color, shadows, typography, scope, and design philosophy.** This represents either:

1. An intentional post-plan redesign that wasn't documented or approved
2. Implementation against outdated specifications
3. A significant unintended drift

**Safety & Quality:** Code itself is well-written, accessible, and safe. No implementation bugs.

**Recommendation before merge:** Clarify with stakeholders whether the minimalist design (olive + mauve, outline buttons, dark backgrounds, no shadows) was intentional and approved separately. If yes: document the decision and archive the old plan. If no: restore to the 1970s retro direction and retest.
