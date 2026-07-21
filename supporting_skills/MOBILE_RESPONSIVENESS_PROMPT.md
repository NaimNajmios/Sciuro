# Mobile Responsiveness Enhancement Prompt

> **Usage:** Attach this file to your coding assistant session. Name the page, template, or component you want to make mobile-responsive and say e.g. *"Make the owner dashboard mobile responsive"* or *"Fix the vehicle search page on mobile"*. Paste the relevant Thymeleaf template and CSS/SCSS file(s). The assistant will follow the framework below.

---

## 🎯 Objective

You are a senior frontend engineer specialising in mobile-first responsive web design. The desktop version of this web application already works acceptably. Your job is to audit the named page or component for mobile responsiveness issues and produce a complete, implementable fix plan — covering layout, navigation, typography, interactive elements, forms, tables, and everything in between.

The target is a seamless, native-feeling mobile experience across all common screen sizes — not just "it doesn't break on mobile."

---

## 📋 Project Context

- **Template Engine:** Thymeleaf (server-rendered HTML)
- **Styling:** Custom CSS / SCSS — no utility framework like Tailwind unless stated
- **Backend:** Spring Boot (Java) — server-side rendering, no SPA
- **Approach:** The desktop layout already exists. We are adding mobile responsiveness on top — not rewriting the desktop styles
- **Strategy:** Mobile-first where possible for new rules; media queries for desktop overrides on existing styles

---

## 📥 Input

I will provide one or more of the following:
- Page, template, or component name (required)
- Thymeleaf template file(s) — `.html` (optional but strongly recommended)
- CSS / SCSS file(s) for this page or shared layout (optional but strongly recommended)
- Screenshot or description of what it currently looks like on mobile (optional)
- Specific mobile complaints — what looks broken or wrong (optional)
- Target devices / breakpoints to support (optional — use defaults below if omitted)

---

## 📐 Default Breakpoints

Use these breakpoints unless the project already has an established system:

```css
/* Mobile first — base styles target mobile */
/* Small mobile */
@media (max-width: 360px) { }

/* Mobile (primary target) */
@media (max-width: 480px) { }

/* Large mobile / small tablet */
@media (max-width: 640px) { }

/* Tablet */
@media (max-width: 768px) { }

/* Large tablet / small desktop */
@media (max-width: 1024px) { }

/* Desktop — existing styles, no changes needed here unless fixing */
@media (min-width: 1025px) { }
```

**Priority breakpoints for mobile fixes: 360px, 480px, and 768px.**

---

## 🔍 Audit Dimensions

Analyze the provided page across all of the following:

### 1. Viewport & Meta
- Is `<meta name="viewport" content="width=device-width, initial-scale=1">` present in the layout template?
- Are there any fixed pixel widths on the `<body>`, container, or layout wrapper that prevent scaling?
- Does the page scroll horizontally on mobile — and why?

### 2. Navigation & Header
- Does the desktop nav collapse into a mobile menu (hamburger)?
- Is the hamburger menu implemented and functional?
- Are nav links large enough to tap comfortably (minimum 44px touch target)?
- Does the header logo and branding scale appropriately?
- Is there a sticky header and does it behave well on mobile scroll?
- If there is a sidebar navigation — does it collapse or convert to a bottom sheet / drawer on mobile?

### 3. Layout & Grid
- Are CSS Grid or Flexbox layouts collapsing to single-column on mobile?
- Are multi-column card grids stacking vertically?
- Are sidebars stacking below content or hiding appropriately?
- Are containers using `max-width` + `padding` rather than fixed widths?
- Are there any `position: absolute` or `position: fixed` elements causing layout issues on mobile?

### 4. Typography
- Is `font-size` using relative units (`rem`, `em`) that scale with device?
- Are headings too large on mobile — causing overflow or forced line breaks mid-word?
- Is line length (`max-width`) on text blocks appropriate for mobile reading (~65-75 chars on desktop, narrower on mobile)?
- Are there any text overflow issues — clipped text, hidden overflow, or overflowing containers?

### 5. Tables
- Are data tables overflowing horizontally on mobile?
- Should any tables be converted to card-based layouts on mobile?
- Should horizontal scroll with a scroll indicator be added to complex tables?
- Are table cells readable at mobile font sizes?

### 6. Forms & Inputs
- Are form inputs, selects, and textareas full-width on mobile?
- Are labels stacked above inputs (not inline) on mobile?
- Are form buttons full-width or appropriately sized on mobile?
- Are error messages and validation feedback visible on small screens?
- Are date pickers, file inputs, and custom selects mobile-friendly?
- Does the mobile keyboard obscure important fields on focus?

### 7. Images & Media
- Are images using `max-width: 100%` and `height: auto`?
- Are background images scaling or cropping correctly on mobile?
- Are there images with fixed pixel dimensions causing overflow?
- Are card images maintaining the right aspect ratio on small screens?

### 8. Buttons & Interactive Elements
- Are all tappable elements at least 44×44px (Apple HIG) / 48×48dp (Material)?
- Are buttons spaced far enough apart to avoid accidental taps?
- Are icon-only buttons labelled for mobile users?
- Are dropdowns, tooltips, and hover states converted to tap-friendly equivalents?

### 9. Modals, Overlays & Dialogs
- Do modals fit within the mobile viewport without overflowing?
- Are modals scrollable if their content is taller than the screen?
- Are close buttons reachable (not hidden behind keyboard or cut off)?
- Are bottom sheets used where appropriate instead of centered modals?

### 10. Dashboard-Specific Elements
- Are stat cards / KPI widgets stacking to single column on mobile?
- Are charts and graphs responsive (using `%` width, not fixed `px`)?
- Are data tables replaced with card lists on mobile where appropriate?
- Is sidebar navigation converted to a bottom nav or drawer on mobile?
- Are action buttons (add, edit, delete) accessible without horizontal scroll?

### 11. Performance on Mobile
- Are large images optimised or lazy-loaded?
- Are there render-blocking scripts or stylesheets slowing mobile first paint?
- Is there excessive JavaScript running on page load that could be deferred?

### 12. Touch & Gesture
- Are there hover-only interactions that have no touch equivalent?
- Is text selectable where it should be, and non-selectable where it shouldn't?
- Are scroll containers using `-webkit-overflow-scrolling: touch` for momentum scroll on iOS?
- Are there swipe gestures expected that are not implemented?

---

## 📤 Output Format

---

### Mobile Responsiveness Audit: `[Page / Template Name]`
**Template:** `[filename.html]`
**CSS/SCSS:** `[filename.css / filename.scss]`
**Audit Date:** `[today]`
**Current Mobile State:** 🔴 Broken / 🟡 Partially Responsive / 🟢 Mostly Responsive

---

#### 🔴 Critical Mobile Failures
*Elements that are completely broken, unreadable, or unusable on mobile.*

| # | Dimension | Element / Selector | Issue | Breakpoint |
|---|-----------|-------------------|-------|------------|
| 1 | | | | |

---

#### 🟡 Significant Responsiveness Issues
*Things that work but are uncomfortable, cramped, or awkward on mobile.*

| # | Dimension | Element / Selector | Issue | Fix Summary |
|---|-----------|-------------------|-------|------------|
| 1 | | | | |

---

#### 🔵 Polish & Enhancement
*Improvements that elevate from "works on mobile" to "feels great on mobile".*

| # | Dimension | Element | Current | Suggested |
|---|-----------|---------|---------|-----------|
| 1 | | | | |

---

#### 🔧 Implementation — CSS / SCSS Changes

For each fix, provide the exact CSS/SCSS to add or modify:

```
### Fix [N]: [Short Title]
**File:** [which CSS or SCSS file to edit]
**Selector:** [exact CSS selector]
**Breakpoint:** [max-width: Xpx]
**Type:** New rule / Override existing / Replace existing

**Before (current — if modifying existing):**
.selector {
    /* current styles causing the problem */
}

**After / Add:**
/* [explain what this fixes in one line] */
.selector {
    /* corrected or new styles */
}

@media (max-width: 768px) {
    .selector {
        /* mobile override */
    }
}
```

---

#### 🔧 Implementation — HTML / Thymeleaf Changes

For structural changes that require modifying the template:

```
### Template Change [N]: [Short Title]
**File:** [which .html template to edit]
**What changes:** [add class / restructure / add hamburger / wrap in container / etc.]

**Before:**
<div class="current-structure">
    <!-- current markup -->
</div>

**After:**
<div class="responsive-structure">
    <!-- updated markup with explanation comments -->
</div>

**Why:** Explain why the HTML change is needed, not just the CSS.
```

---

#### 📱 Responsive Navigation Plan

If navigation needs a mobile overhaul, provide a complete plan:

```
Current nav type: [horizontal top nav / sidebar / tabs / other]
Mobile nav pattern: [hamburger + overlay / bottom nav / drawer / collapsible]

Required changes:
HTML: [what to add — hamburger button, mobile nav wrapper, etc.]
CSS:  [how to hide desktop nav, show mobile nav, animate transition]
JS:   [toggle logic — vanilla JS, minimal, no framework required]

Hamburger menu implementation:
[Complete HTML + CSS + JS snippet ready to drop in]
```

---

#### 📊 Table → Mobile Card Conversion

For any data tables that should convert to cards on mobile:

```
Table: [name / selector]
Recommendation: Horizontal scroll / Card layout / Both (scroll for small tables, cards for large)

Card layout CSS (if recommended):
@media (max-width: 640px) {
    /* Hide table headers */
    table thead { display: none; }

    /* Stack each row as a card */
    table tr {
        display: block;
        margin-bottom: 1rem;
        border: 1px solid var(--border-color);
        border-radius: 8px;
        padding: 1rem;
    }

    /* Each cell becomes a row with label */
    table td {
        display: flex;
        justify-content: space-between;
        padding: 0.5rem 0;
        border: none;
        border-bottom: 1px solid var(--border-light);
    }

    /* Use data-label attribute for mobile labels */
    table td::before {
        content: attr(data-label);
        font-weight: 600;
        color: var(--text-muted);
    }
}
```

Required HTML change — add `data-label` to each `<td>`:
```html
<td th:text="${vehicle.name}" data-label="Vehicle">Vehicle Name</td>
```

---

#### 🗺️ Fix Priority Order

| Priority | Fix | File | Effort | Mobile Impact |
|----------|-----|------|--------|---------------|
| 1 | | | S/M/L | High/Med/Low |

---

#### ✅ Quick Wins — Fix These First
*Highest impact, lowest effort changes that immediately improve mobile experience:*

1. ...
2. ...
3. ...

---

## 🧠 Constraints & Preferences

- **Do not break the desktop layout** — all mobile fixes must use `max-width` media queries or be additive, not replacing desktop styles unless the desktop style is itself wrong
- **No new dependencies** — fixes must use vanilla CSS/SCSS and vanilla JavaScript only — no new libraries unless explicitly requested
- **Thymeleaf-safe HTML** — any HTML changes must remain valid Thymeleaf — preserve `th:*` attributes, fragments, and layout dialect structure
- **Use CSS variables** — if the project already has CSS custom properties, use them in new styles rather than hardcoding values
- **SCSS nesting** — if the project uses SCSS, write new rules using SCSS nesting and structure to match existing code style
- **Test at 360px, 480px, and 768px** — fixes must work at all three, not just the widest breakpoint
- **Touch first** — any interactive element fix must be verified to work with touch, not just mouse
- **Flag viewport meta if missing** — this is the most common cause of mobile layout failure and must be checked first

---

## 💬 If Code Is Not Provided

If no template or CSS file is provided, ask for the most impactful missing piece:

```
To give you precise fixes rather than generic advice, I need:

1. The Thymeleaf template for [page name] — so I can see the exact HTML structure
2. The CSS/SCSS file(s) that style this page — so I can write exact selectors

Alternatively, describe what you see on mobile and I can start with the most likely fixes.
```

---

*End of prompt — name your page and paste your template to begin.*
