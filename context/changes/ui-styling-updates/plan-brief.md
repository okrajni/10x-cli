# UI Styling Updates — Plan Brief

> Full plan: `context/changes/ui-styling-updates/plan.md`

## What & Why

Complete visual redesign of the "done yet?" task app from default Tailwind blue theme to a warm 1970s retro aesthetic. The new palette uses cream (#E8E2D4) as the primary background, forest green (#436741) for primary actions, and dusty rose (#E1C1C6) for accents, paired with Playfair Display serif typography for headlines. The goal is to create a cohesive, nostalgic experience that feels inviting and distinctly retro across all 8 pages and 7 core components.

## Starting Point

The app currently uses Tailwind's default color scheme (blue brand, gray neutrals, semantic reds/greens) with no custom theme configuration. All styling is done via utility classes directly in JSX. Playfair Display is not integrated. The codebase has a clear component architecture (shared components + feature-specific components), making a systematic redesign straightforward.

## Desired End State

After completing this plan:
- All pages display the new retro palette (cream/green/rose) with no blue color remnants
- All h1, h2, h3 headings use Playfair Display serif font; body text remains sans-serif
- Cards and buttons have soft offset shadows and subtle borders for tactile 1970s feel
- Button hover/active states use darker color variants with smooth transitions
- All text meets WCAG AA contrast requirements (4.5:1 minimum)
- Responsive design works identically on mobile, tablet, and desktop
- No functional changes — all features work as before

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| **Color distribution** | Cream primary (backgrounds), green accent (primary actions), rose secondary (accents) | Cream is warm and welcoming; green provides strong visual hierarchy; rose adds nostalgic warmth. Creates cohesive retro feel. | Plan |
| **Interactive colors** | Replace all blues with green/rose system | Full palette consistency; no jarring blue remnants. Green and rose darkening works well for hover/active states. | Plan |
| **Typography scope** | Playfair Display serif for h1-h3 headlines only; sans-serif for body/inputs | Classic retro magazine aesthetic; maintains readability and usability. Automatic application via Tailwind `@layer base` minimizes code changes. | Plan |
| **Retro era** | 1970s warm nostalgia | User's color palette (cream, soft green, dusty rose) naturally fits this era. Soft shadows and 6px rounded corners support authentic warm vibe. | Plan |
| **Category colors** | Keep muted versions of existing palette (blue→green, purple→cream, etc.) | Preserves visual distinction between categories while fitting new palette. Less chaotic than rotating all three colors. | Plan |
| **Card styling** | Soft shadows + subtle borders + 6px rounded corners | Tactile, warm retro feel without clutter. Soft offset shadows (not blurred) are authentically 1970s. | Plan |
| **Dark mode** | Light-only (no dark mode support) | Retro aesthetic is inherently light-based (warm creams/roses). Simpler implementation. | Plan |
| **Interactive states** | Darker/lighter tint + subtle shift | Green: #436741 → hover #2F4A2E → pressed #243620; Rose: #E1C1C6 → hover #C9A4AA → pressed #B08A90. Preserves retro feel. | Plan |
| **Accessibility** | WCAG AA (4.5:1 contrast minimum) | Required for compliance and usability. Text colors: charcoal on cream/rose ✅, white on green ✅. Never charcoal on dark green (fails AA). | Plan |

## Scope

**In scope:**
- Tailwind theme customization (colors, fonts, shadows)
- All 8 pages and 7 core components styling update
- Typography refresh (Playfair Display for headlines)
- Accessibility verification (WCAG AA compliance)

**Out of scope:**
- Layout changes (grids, sidebar width, spacing remain unchanged)
- New components or functionality
- Dark mode support
- Responsive breakpoint changes
- Animation or interaction behavior changes

## Architecture / Approach

**Bottom-up phased customization:**

1. Configure Tailwind theme first (colors, fonts, shadows) — the infrastructure
2. Update shared components (Button, Input, Dialog, Card)
3. Update feature components and pages
4. Add retro visual polish (shadows, borders, corner radius)
5. Test comprehensively (accessibility, cross-browser, responsive)

**Design tokens:** All colors, fonts, and shadows are centralized in `tailwind.config.js` and `index.css`. No magic numbers or scattered color references.

**Font application:** Playfair Display imported from Google Fonts, applied to h1-h3 via Tailwind `@layer base` — automatic, no DOM class additions needed.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Tailwind Theme | Custom color palette, font stack, shadow system configured in `tailwind.config.js` | If config has syntax errors, build fails. Caught by TypeScript + build checks. |
| 2. Typography | Playfair Display imported and applied to all h1-h3 headings | Font may not load (network issue). Mitigated with `display=swap` parameter. |
| 3. Core Components | Button, Input, Dialog, Card updated to green/rose colors | Color choices may have contrast issues. Caught during Phase 6 accessibility audit. |
| 4. Feature Swap | All 8 pages and feature components updated to new palette | May miss some blue references if scattered throughout code. Caught by manual visual review. |
| 5. Retro Polish | Soft shadows, subtle borders, corner radius refinement | Shadows may not feel "authentic" without fine-tuning offsets. Addressed through manual visual feedback. |
| 6. Testing | Accessibility audit, cross-browser testing, responsive verification | Discovered contrast violations late in process. Mitigated by Phase 1 planning. |

**Prerequisites:** Bun/Node installed, frontend environment set up, ability to run `npm run build` and `npm run dev`.

**Estimated effort:** ~3-4 sessions across 6 phases (assuming ~1 session per 2 phases for implementation + testing).

## Open Risks & Assumptions

- **Assumption**: All inline color references in code are catchable by manual review or linting. If colors are hardcoded in many places, Phase 4 could take longer.
- **Risk**: Google Fonts Playfair Display may load slowly on slower connections. Mitigated with `display=swap` to prevent blocking.
- **Risk**: Contrast issues discovered late in testing (Phase 6). Mitigated by upfront planning of text color rules in Phase 1.
- **Assumption**: No existing dark mode code or CSS that conflicts with light-only design. If dark mode CSS exists, it should be left unchanged (Phase 1 constraint).

## Success Criteria (Summary)

1. **Visual consistency**: All pages display cream/green/rose palette with no blue remnants; typography hierarchy clear with serif h1-h3
2. **Accessibility**: All text meets WCAG AA (4.5:1 minimum); focus rings visible on all interactive elements
3. **Functional integrity**: No regressions — all features (create/edit/delete tasks, login, navigation) work exactly as before
4. **Aesthetic achievement**: Overall design feels warm, inviting, and authentically 1970s retro across all viewports
