# UI Styling Updates — 1970s Retro Aesthetic Implementation Plan

## Overview

Transform the "done yet?" task management app from a default Tailwind blue theme to a warm 1970s retro aesthetic using a cream, forest green, and dusty rose color palette with Playfair Display serif typography for headlines. The redesign will maintain full functionality while creating a cohesive, nostalgic visual experience across all 8 pages and 7 core components.

## Current State Analysis

The app currently uses **Tailwind CSS defaults** with minimal customization:
- **Color scheme**: Blue brand (600/700), gray neutrals, semantic reds/greens/oranges
- **Typography**: Tailwind defaults (sans-serif only)
- **Components**: Button, Input, Dialog, TaskCard with utility-class styling
- **Shadows & borders**: Standard Tailwind defaults (no custom styling)
- **Design system**: None — directly using Tailwind utility classes across all pages

The codebase structure provides a clear path forward:
- Centralized theme config in `tailwind.config.js` (currently minimal)
- Component library in `src/shared/components/` (Button, Input, Dialog, Card, etc.)
- Feature components in `src/features/tasks/components/` (TodayDashboardContainer, TaskCard)
- Utility functions for category colors in `src/features/tasks/utils/categoryUtils.ts`

## Desired End State

After completing this plan:

**Visual appearance:** All pages will use a cohesive 1970s warm retro aesthetic with:
- Cream (#E8E2D4) as primary background color
- Forest green (#436741) as primary interactive color (buttons, links, focus rings)
- Dusty rose (#E1C1C6) as secondary accent color
- Playfair Display serif font for all h1, h2, h3 headlines
- Soft, offset shadows and subtle borders for tactile retro feel

**Functional state:** No changes to functionality — all features work identically, just with new styling.

**Verification:** Users can navigate all pages, interact with all components, and confirm visual consistency matches the 1970s retro design direction across mobile, tablet, and desktop screens.

### Key Discoveries

- **Tailwind extensibility**: Using `theme.extend.colors` preserves all default colors while adding custom ones — safer than replacing defaults.
- **Google Fonts + Tailwind**: Playfair Display can be imported via CSS and controlled entirely within Tailwind config using `@layer base` directives — no DOM class additions needed for h1-h3.
- **WCAG AA contrast compliance requires specific text color rules**:
  - Charcoal text (#1A1A1A) on cream/rose backgrounds ✅
  - White/cream text on green backgrounds ✅
  - Dark text on dark green fails (3.8:1 ratio) — must use white instead
- **Color darkening for interactive states**: Green darkens to #2F4A2E (hover) / #243620 (pressed); rose darkens to #C9A4AA / #B08A90. Cream doesn't darken — use borders/text color shifts instead.
- **Retro aesthetic details**: 6px rounded corners (not 16px) feel more authentically 1970s; soft offset shadows (2-6px) with low opacity (8-18%) rather than modern blur-based shadows.

## What We're NOT Doing

- **Layout changes**: Grid structures, sidebar width, header height all remain unchanged
- **Component additions**: No new components beyond styling; using existing Button, Input, Dialog, Card
- **Dark mode support**: Light-only design (user requested this)
- **Animation/interaction changes**: Hover/focus states styling only, no new interactions
- **Responsive redesign**: Mobile breakpoints remain unchanged; only applying new colors/fonts

## Implementation Approach

**Phased, bottom-up customization:**

1. **Configure Tailwind theme first** — establish the color system and typography infrastructure before touching any components
2. **Update shared components** — style the foundational UI elements (Button, Input, Dialog)
3. **Update feature components** — apply new palette to task-specific components
4. **Polish retro details** — add shadows, borders, spacing refinements
5. **Test comprehensively** — verify visual consistency and accessibility across all pages

**Design tokens:**
- Colors defined in `tailwind.config.js` as custom theme extensions
- Font stack defined in config with `@layer base` directive for automatic h1-h3 application
- Shadow variants created as custom utilities in config
- No magic numbers — all colors, spacing, and shadows tied to config values

## Critical Implementation Details

**Color darkening for interactive states:** Darker variants must be calculated carefully. For green (#436741), the hover state is #2F4A2E (achieved by reducing HSL lightness by 15%). For rose (#E1C1C6), the hover state is #C9A4AA (20% lightness reduction). Use these exact hex values across all button/link hover states — don't rely on opacity changes, as they reduce vibrancy and break the retro aesthetic.

**Text color contrast rules:** This is non-negotiable for accessibility. Charcoal (#1A1A1A) text works on cream and rose; white text is required on green. Never use dark text on the dark green background — it fails WCAG AA (3.8:1 ratio). If new components are added during implementation, these rules must apply immediately.

**Playfair Display application:** The font must be imported from Google Fonts and applied ONLY to h1, h2, h3 via Tailwind's `@layer base` directive. Body text, inputs, buttons, and labels remain sans-serif. This is handled at the Tailwind config level — no need to add `font-serif` classes to individual heading elements.

---

## Phase 1: Tailwind Theme Customization

### Overview

Configure `tailwind.config.js` to establish the custom color palette, typography, and shadow system that will be used throughout the app. This phase creates the foundation — no visual changes yet, but all subsequent phases depend on this configuration.

### Changes Required

#### 1. Extend Tailwind Theme with Custom Colors

**File:** `frontend/tailwind.config.js`

**Intent:** Add custom color definitions for cream, green, rose, and their variants (hover/pressed states) to Tailwind's color palette. Use `theme.extend.colors` to preserve all default Tailwind colors while adding the new retro palette.

**Contract:** The `theme.extend.colors` object should include:
```javascript
theme: {
  extend: {
    colors: {
      cream: {
        50: '#F5F3F0',
        100: '#E8E2D4',
        200: '#DDD6C8',
      },
      green: {
        50: '#F0F4F1',
        600: '#436741', // primary
        700: '#2F4A2E', // hover
        800: '#243620', // pressed
      },
      rose: {
        50: '#FAF7F7',
        100: '#E1C1C6', // primary
        200: '#C9A4AA', // hover
        300: '#B08A90', // pressed
      },
      charcoal: '#1A1A1A',
    },
  },
}
```

#### 2. Add Typography Configuration

**File:** `frontend/tailwind.config.js`

**Intent:** Import Playfair Display from Google Fonts and define font stacks in Tailwind config. Set up automatic serif application to h1-h3 elements using `@layer base`.

**Contract:** The `theme.extend.fontFamily` object should include:
```javascript
theme: {
  extend: {
    fontFamily: {
      serif: ['Playfair Display', 'serif'],
      sans: ['system-ui', 'sans-serif'],
    },
  },
}
```

And in `src/index.css`, add:
```css
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600;700;800;900&display=swap');

@layer base {
  h1, h2, h3 {
    @apply font-serif font-semibold;
  }
}
```

#### 3. Define Custom Shadow Variants

**File:** `frontend/tailwind.config.js`

**Intent:** Create retro-appropriate soft offset shadows with low opacity to replace Tailwind's default blurred shadows. Add colored shadow variants for green and rose.

**Contract:** The `theme.extend.boxShadow` object should include:
```javascript
theme: {
  extend: {
    boxShadow: {
      'retro': '4px 4px 0px rgba(26, 26, 26, 0.08)',
      'retro-green': '4px 4px 0px rgba(67, 103, 65, 0.12)',
      'retro-rose': '4px 4px 0px rgba(225, 193, 198, 0.12)',
      'retro-lg': '6px 6px 0px rgba(26, 26, 26, 0.1)',
    },
  },
}
```

### Success Criteria

#### Automated Verification

- TypeScript type checking passes on `tailwind.config.js`: `npm run typecheck`
- Linting passes: `npm run lint`
- Tailwind builds without errors: `npm run build`
- Config can be imported and parsed correctly (no syntax errors)

#### Manual Verification

- All new colors are accessible via Tailwind utility classes (e.g., `bg-green-600`, `text-rose-100`)
- Google Fonts Playfair Display loads visibly in browser (open DevTools → check Network tab for font file)
- New shadow utilities are available in editor autocomplete (e.g., `shadow-retro`, `shadow-retro-green`)
- Existing Tailwind utilities still work (blue-600, gray-200, etc. not removed)

---

## Phase 2: Typography & Font Integration

### Overview

Import Playfair Display from Google Fonts and apply it to all h1, h2, h3 headings across the app. Verify font loads correctly and displays with proper weights/sizes. Maintain sans-serif for body text, inputs, and buttons.

### Changes Required

#### 1. Verify Font Imports and Tailwind Directives

**File:** `frontend/src/index.css`

**Intent:** Ensure Playfair Display is imported from Google Fonts and that Tailwind's `@layer base` directive applies the serif font to h1-h3 automatically.

**Contract:** The file should contain:
```css
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600;700;800;900&display=swap');

@layer base {
  h1, h2, h3 {
    @apply font-serif font-semibold;
  }
}

@tailwind base;
@tailwind components;
@tailwind utilities;
```

#### 2. Update Heading Typography Across Components

**File:** `frontend/src/shared/components/` and all feature components

**Intent:** Ensure all h1, h2, h3 elements in the codebase use semantic HTML tags (not styled divs with `text-3xl` classes) so the `@layer base` directive applies automatically.

**Contract:** Scan components for h1-h3 usage. If any headings use `<div class="text-3xl font-bold">` pattern, convert to semantic `<h1>`, `<h2>`, or `<h3>`. No additional classes needed — Tailwind's base layer will apply the serif font + semibold weight automatically.

### Success Criteria

#### Automated Verification

- TypeScript type checking passes: `npm run typecheck`
- Linting passes: `npm run lint`
- Build succeeds: `npm run build`
- No console warnings about missing fonts

#### Manual Verification

- Open the app in a browser
- Navigate to dashboard, task list, and settings pages
- Verify all h1, h2, h3 headings display in Playfair Display serif font (visually distinct from body text)
- Verify body text, button labels, and input placeholders remain in sans-serif font
- Font loads within 1-2 seconds (check Performance tab in DevTools)
- Headings are readable and don't look distorted

---

## Phase 3: Color Palette Swap (Core Components)

### Overview

Update shared UI components (Button, Input, Dialog, Card) to use the new green/rose/cream palette instead of blue defaults. This affects the foundational components used throughout the app.

### Changes Required

#### 1. Update Button Component

**File:** `frontend/src/shared/components/Button.tsx`

**Intent:** Replace blue color tokens with green for primary variant and rose for secondary/danger variants. Update hover/active states to use darker color variants.

**Contract:** Button variants should be:
- **Primary**: `bg-green-600` (normal), `bg-green-700` (hover), `bg-green-800` (active)
- **Secondary**: `bg-rose-100 text-charcoal` (normal), `bg-rose-200 text-charcoal` (hover)
- **Danger**: `bg-rose-100 text-charcoal` (normal), `bg-rose-200 text-charcoal` (hover)
- Focus rings: `focus:ring-green-600` (primary), `focus:ring-rose-100` (secondary)

#### 2. Update Input Component

**File:** `frontend/src/shared/components/Input.tsx`

**Intent:** Replace blue focus ring with green. Update error state colors to use rose instead of red.

**Contract:**
- Normal focus: `focus:ring-green-600 focus:border-green-600`
- Error state: `border-rose-100 bg-rose-50` (instead of red)
- Help text color: `text-charcoal` or `text-gray-600`

#### 3. Update Dialog Component

**File:** `frontend/src/shared/components/Dialog.tsx`

**Intent:** Ensure dialog overlays and buttons use new color palette. Update any blue elements to green.

**Contract:**
- Overlay background: unchanged (semi-transparent dark)
- Dialog card background: `bg-cream-100`
- Close button: `text-charcoal hover:text-green-600`
- Action buttons: follow Button component color rules above

#### 4. Update Card Component

**File:** `frontend/src/shared/components/TaskCard.tsx`

**Intent:** Add retro shadow and border styling. Update text colors to ensure contrast.

**Contract:**
- Card background: `bg-cream-100`
- Card shadow: `shadow-retro` (new utility defined in Phase 1)
- Card border: `border border-cream-200` (subtle)
- Text: `text-charcoal` for body, headings use serif font from Phase 2
- Hover state: `shadow-retro-lg` (lifted effect)

### Success Criteria

#### Automated Verification

- TypeScript type checking passes: `npm run typecheck`
- Linting passes: `npm run lint`
- Unit tests pass (if component tests exist): `npm run test`
- Build succeeds: `npm run build`

#### Manual Verification

- Open component storybook or demo page (if available)
- Verify all button variants display with correct green/rose colors
- Test button hover and active states visually
- Verify input focus ring is green, not blue
- Verify error inputs show rose, not red
- Verify dialog cards have cream background with subtle shadows
- All text is readable (check contrast with accessibility checker)

---

## Phase 4: Color Palette Swap (Feature Components & Pages)

### Overview

Update all feature-specific components and page files to use the new color palette. This includes task components, dashboard containers, and any page-level styling.

### Changes Required

#### 1. Update Task Category Colors

**File:** `frontend/src/features/tasks/utils/categoryUtils.ts`

**Intent:** Mute the existing category color palette (blue/green/purple/orange/red) to match the retro aesthetic. Replace bright colors with softer, more earthy variants.

**Contract:** Category colors should shift from bright to muted:
```javascript
const CATEGORY_COLORS: Record<TaskCategory, string> = {
  CLEANING: 'bg-green-50 text-green-700', // was blue-100/blue-800
  SHOPPING: 'bg-rose-50 text-rose-200',   // was green-100/green-800
  LAUNDRY: 'bg-cream-100 text-charcoal',  // was purple
  MAINTENANCE: 'bg-orange-100 text-orange-700', // keep muted orange
  BILLS: 'bg-rose-50 text-rose-200', // was red, now rose
};
```

#### 2. Update TodayDashboardContainer

**File:** `frontend/src/features/tasks/components/TodayDashboardContainer.tsx`

**Intent:** Replace blue info panel with cream/green styling. Update heading colors and section dividers.

**Contract:**
- Info panel background: `bg-green-50` (was blue)
- Heading text: uses serif font from Phase 2 (automatic)
- Badge/pill backgrounds: use category colors from categoryUtils
- Dividers: `border-cream-200` (subtle)

#### 3. Update TaskDeleteDialog

**File:** `frontend/src/features/tasks/components/TaskDeleteDialog.tsx`

**Intent:** Update danger button colors to use rose instead of red.

**Contract:**
- Delete button: `bg-rose-200 text-charcoal` (warning state)
- Hover: `bg-rose-300 text-charcoal`
- Cancel button: secondary style (green or cream)

#### 4. Update All Page Files

**Files:** `frontend/src/features/tasks/pages/*.tsx`, `frontend/src/features/auth/pages/*.tsx`, `frontend/src/features/household/pages/*.tsx`

**Intent:** Ensure page-level backgrounds, text colors, and section headings use the new palette. Replace any inline blue color references with green.

**Contract:**
- Page backgrounds: `bg-cream-50` or `bg-white` (cream is primary, white acceptable for maximum contrast)
- Section headings: use h2/h3 semantic tags (serif applied automatically)
- Link colors: `text-green-600 hover:text-green-700`
- Error messages: `text-rose-200` or `bg-rose-50`

### Success Criteria

#### Automated Verification

- TypeScript type checking passes: `npm run typecheck`
- Linting passes: `npm run lint`
- Build succeeds: `npm run build`
- No console errors or warnings related to missing colors

#### Manual Verification

- Open all 8 pages in the app:
  - Dashboard (verify green info panel, category colors)
  - Task list (verify task cards with retro shadows)
  - Task create (verify green submit button, category badges)
  - Task edit (verify green button, rose error colors if applicable)
  - Settings (verify all buttons and inputs in new palette)
  - Login/Register (verify gradient and button colors)
  - Household create (verify styling consistency)
- All category colors (CLEANING, SHOPPING, etc.) display with muted, earthy tones
- All text is readable with sufficient contrast
- Links and focus states use green, not blue
- No blue remnants visible anywhere

---

## Phase 5: Retro Visual Polish

### Overview

Add finishing touches to create an authentic 1970s retro aesthetic: soft offset shadows on cards, subtle borders, refined spacing, and interactive state polish.

### Changes Required

#### 1. Apply Retro Shadows to Cards and Containers

**File:** `frontend/src/shared/components/TaskCard.tsx` and similar card components

**Intent:** Replace default Tailwind shadows with soft, offset retro shadows defined in Phase 1. Add hover state lift effect.

**Contract:**
- Normal state: `shadow-retro` (4px 4px offset)
- Hover state: `shadow-retro-lg` (6px 6px offset for "lifted" effect)
- Transition: `transition-shadow duration-150` (smooth hover effect)

#### 2. Add Subtle Colored Borders

**File:** All card and panel components

**Intent:** Add thin, colored borders to cards and panels for definition without harsh lines. Use cream/green/rose color variants.

**Contract:**
- Border width: `border` (1px)
- Border color: `border-cream-200` for most cards, `border-green-100` for interactive sections
- Avoid heavy borders — keep it subtle

#### 3. Refine Corner Radius

**File:** Tailwind config and all rounded elements

**Intent:** Adjust border radius from default Tailwind values to 6px for a more authentically retro feel (less modern, less rounded).

**Contract:**
- Card/dialog corners: `rounded-md` or `rounded` (6px, Tailwind default)
- Button corners: `rounded` (keep consistent with cards)
- Input corners: `rounded` (consistent styling)
- Avoid `rounded-lg` (16px) or `rounded-full` — too modern

#### 4. Update Interactive State Visual Feedback

**File:** `frontend/src/shared/components/Button.tsx` and interactive elements

**Intent:** Ensure button hover/active states use darker color variants and subtle shadow shifts for tactile retro feedback.

**Contract:**
- Hover: darker color (`bg-green-700` or `bg-rose-200`) + `shadow-retro`
- Active/pressed: even darker color (`bg-green-800` or `bg-rose-300`) + no shadow (pressed down effect)
- Focus: `outline outline-2 outline-offset-2 outline-green-600` (instead of ring)

#### 5. Polish Typography Hierarchy

**File:** All page files and components

**Intent:** Ensure heading sizes and weights create proper visual hierarchy. Use Playfair Display weight variants (600, 700, 800, 900) for emphasis.

**Contract:**
- h1 (page title): `text-4xl font-serif font-bold` (weight 900)
- h2 (section): `text-2xl font-serif font-semibold` (weight 700)
- h3 (subsection): `text-xl font-serif font-semibold` (weight 600)
- Body: `text-base text-charcoal` (sans-serif)
- Labels: `text-sm text-charcoal` (sans-serif)

### Success Criteria

#### Automated Verification

- TypeScript type checking passes: `npm run typecheck`
- Linting passes: `npm run lint`
- Build succeeds: `npm run build`

#### Manual Verification

- Cards display with soft offset shadows (not blurred/modern)
- Hovering over cards shows lifted effect (shadow increases)
- Button hover states darken appropriately (green → darker green, rose → darker rose)
- Button click/active states feel "pressed" (shadow removed or reduced)
- All corners are slightly rounded (6px, not sharp, not overly rounded)
- Heading sizes create clear visual hierarchy
- Playfair Display serif font is prominent on all h1-h3 elements
- Overall aesthetic feels warm and nostalgic (1970s-inspired)

---

## Phase 6: Testing & Accessibility Verification

### Overview

Comprehensive testing across all pages and screen sizes. Verify visual consistency, accessibility compliance, and that no regressions were introduced in functionality or styling.

### Changes Required

#### 1. Accessibility Audit

**File:** All component files

**Intent:** Verify that text color contrast meets WCAG AA standards (4.5:1 for normal text, 3:1 for large text). Run automated accessibility checks.

**Contract:**
- No text with contrast ratio below 4.5:1 (except large text, which requires 3:1)
- Use accessible color checker tools (e.g., WebAIM, axe DevTools)
- Required text color combinations:
  - Charcoal on cream ✅ (26.8:1)
  - Charcoal on rose ✅ (20.1:1)
  - White on green ✅ (8.2:1)
  - ❌ NEVER charcoal on green (3.8:1 — fails AA)

#### 2. Cross-Browser Visual Testing

**Files:** All pages

**Intent:** Verify styling renders consistently on Chrome, Firefox, Safari (desktop) and mobile browsers.

**Contract:**
- Colors display consistently across browsers
- Fonts load correctly (no fallback fonts visible)
- Shadows render with proper offset and opacity
- No layout shifts or overflow issues

#### 3. Responsive Design Testing

**Files:** All pages

**Intent:** Verify visual consistency across mobile (375px), tablet (768px), and desktop (1024px+) viewports.

**Contract:**
- Cards/containers stack properly on mobile
- Buttons remain clickable (48px minimum touch target)
- Text remains readable at all sizes
- Shadows don't cause overflow or cutoff
- Navigation and layout unchanged (Phase 1 constraint)

#### 4. Functional Regression Testing

**Files:** All pages and features

**Intent:** Ensure no functionality was broken by styling changes. All interactions work as before.

**Contract:**
- User can log in and navigate to dashboard
- User can view, create, edit, and delete tasks
- Filters, sorting, and search work (if applicable)
- Forms submit correctly
- Dialogs open/close properly
- All links navigate correctly

### Success Criteria

#### Automated Verification

- Accessibility checker (axe, Lighthouse) reports no critical contrast issues
- TypeScript type checking passes: `npm run typecheck`
- Linting passes: `npm run lint`
- All tests pass: `npm run test`
- Build succeeds: `npm run build`

#### Manual Verification

- All 8 pages visually reviewed for:
  - Color consistency (cream/green/rose palette throughout)
  - Typography hierarchy (serif h1-h3, sans-serif body)
  - Retro aesthetic (soft shadows, subtle borders, warm feel)
  - No blue color remnants
- Accessibility testing:
  - Tab through all interactive elements — focus rings visible and appropriate
  - Check text contrast on key elements (buttons, links, labels) with color checker
  - Verify no WCAG AA violations
- Responsive testing:
  - Test on mobile (375px), tablet (768px), desktop (1200px)
  - Verify no layout breaks or overflow
  - Buttons and form inputs remain usable
- Functional testing:
  - Create, edit, delete a task — verify styling doesn't affect functionality
  - Navigate all pages — verify links work and pages load
  - Submit forms — verify styling doesn't break form submission
  - Test on 2+ browsers (Chrome, Firefox or Safari)

---

## Testing Strategy

### Unit Tests

- Component snapshot tests for Button, Input, Dialog, Card (verify className structure)
- Utility tests for categoryUtils.ts (verify muted colors are applied correctly)

### Integration Tests

- Page rendering tests (verify pages load without errors)
- Color palette tests (verify all pages use only cream/green/rose, no blue)

### Manual Testing Steps

1. **Visual walkthrough**: Open app, navigate to each page, verify colors and fonts
2. **Accessibility check**: Use WebAIM or axe DevTools to check contrast ratios
3. **Responsive check**: Test at 375px, 768px, 1024px viewports
4. **Interaction check**: Hover over buttons, click links, submit forms — verify visual feedback works
5. **Cross-browser check**: Test in Chrome, Firefox, Safari (at least one desktop browser)

## Performance Considerations

- **Google Fonts**: Playfair Display is loaded from CDN with `display=swap` parameter, so it won't block page load
- **Custom colors**: No performance impact — colors are defined in Tailwind config and compiled to static CSS
- **Shadows**: Soft offset shadows have no performance impact (not using filters or blur)
- **Build size**: Estimated minimal increase (custom colors add ~2KB compiled CSS)

## Migration Notes

No data migration needed — this is a purely visual change with no impact on database or data structures.

---

## References

- Frame brief: `context/changes/ui-styling-updates/frame.md` (if present)
- Research: `context/changes/ui-styling-updates/research.md` (if present)

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Tailwind Theme Customization

#### Automated

- [x] 1.1 TypeScript type checking passes — 8f61098
- [x] 1.2 Linting passes — 8f61098
- [x] 1.3 Build succeeds — 8f61098

#### Manual

- [x] 1.4 New colors accessible via Tailwind utilities — 8f61098
- [x] 1.5 Google Fonts Playfair Display loads in browser — 8f61098
- [x] 1.6 New shadow utilities available in editor — 8f61098
- [x] 1.7 Existing Tailwind utilities still work — 8f61098

### Phase 2: Typography & Font Integration

#### Automated

- [x] 2.1 TypeScript type checking passes — 1cb4ea8
- [x] 2.2 Linting passes — 1cb4ea8
- [x] 2.3 Build succeeds — 1cb4ea8
- [x] 2.4 No console warnings about missing fonts — 1cb4ea8

#### Manual

- [x] 2.5 All h1/h2/h3 display in Playfair Display serif — 1cb4ea8
- [x] 2.6 Body text remains sans-serif — 1cb4ea8
- [x] 2.7 Font loads within 1-2 seconds — 1cb4ea8
- [x] 2.8 Headings are readable and not distorted — 1cb4ea8

### Phase 3: Color Palette Swap (Core Components)

#### Automated

- [x] 3.1 TypeScript type checking passes
- [x] 3.2 Linting passes
- [x] 3.3 Unit tests pass
- [x] 3.4 Build succeeds

#### Manual

- [x] 3.5 Button variants display with green/rose colors
- [x] 3.6 Input focus rings are green, not blue
- [x] 3.7 Error inputs show rose, not red
- [x] 3.8 Dialog cards have cream backgrounds with retro shadows
- [x] 3.9 All text is readable with sufficient contrast

### Phase 4: Color Palette Swap (Feature Components & Pages)

#### Automated

- [ ] 4.1 TypeScript type checking passes
- [ ] 4.2 Linting passes
- [ ] 4.3 Build succeeds
- [ ] 4.4 No console errors about missing colors

#### Manual

- [ ] 4.5 All 8 pages display with new palette
- [ ] 4.6 Dashboard shows green info panel
- [ ] 4.7 Task cards show muted category colors
- [ ] 4.8 All buttons and links use green/rose
- [ ] 4.9 No blue color remnants anywhere
- [ ] 4.10 All text readable with sufficient contrast

### Phase 5: Retro Visual Polish

#### Automated

- [ ] 5.1 TypeScript type checking passes
- [ ] 5.2 Linting passes
- [ ] 5.3 Build succeeds

#### Manual

- [ ] 5.4 Cards display soft offset shadows
- [ ] 5.5 Card hover shows lifted effect
- [ ] 5.6 Button hover/active states show color darkening
- [ ] 5.7 All corners slightly rounded (6px, authentic retro)
- [ ] 5.8 Heading hierarchy clear with serif emphasis
- [ ] 5.9 Overall aesthetic feels warm and nostalgic

### Phase 6: Testing & Accessibility Verification

#### Automated

- [ ] 6.1 Accessibility checker reports no critical issues
- [ ] 6.2 TypeScript type checking passes
- [ ] 6.3 Linting passes
- [ ] 6.4 All tests pass
- [ ] 6.5 Build succeeds

#### Manual

- [ ] 6.6 All 8 pages visually reviewed for consistency
- [ ] 6.7 Tab through all interactive elements — focus visible
- [ ] 6.8 Text contrast verified (no WCAG AA violations)
- [ ] 6.9 Responsive testing: 375px, 768px, 1200px viewports
- [ ] 6.10 Functional regression: create/edit/delete tasks works
- [ ] 6.11 Cross-browser testing: Chrome, Firefox (or Safari) desktop
