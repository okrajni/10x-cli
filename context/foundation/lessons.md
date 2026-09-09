# Lessons Learned

> Append-only register of recurring rules and patterns. Re-read at start by /10x-frame, /10x-research, /10x-plan, /10x-plan-review, /10x-implement, /10x-impl-review.

## Verify CORS and proxy rewrites in frontend-backend dev setup

- **Context**: Frontend-backend dev server integration with proxy and CORS
- **Problem**: Frontend requests fail with 401 UNAUTHORIZED or are silently blocked by browser when (a) CORS allowlist doesn't include the dev server port, or (b) proxy rewrites strip URL segments the backend expects. Both manifest as auth failures that are hard to debug.
- **Rule**: Always verify CORS allowlist includes all dev server ports (not just production ports); never strip or rewrite API prefixes in proxy rules unless the backend controller explicitly expects it.
- **Applies to**: implement, impl-review
