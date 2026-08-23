# Fold status-bar ownership regression

These screenshots intentionally retain the Android status/notification bar. They document the Fold-specific visual defect reported after the main UX pass and are isolated from the system-bar-free acceptance screenshot sets.

## Root cause

Areas and Review & Trends became full-width destinations below the status bar, but their safe-drawing inset was applied to the outer surface. The transparent status bar therefore continued to reveal the two underlying Fold panes and their center divider.

## Fix

Destination-sized overlays now use the shared `WhipFullScreenSurface`. Its background owns the complete edge-to-edge window; a child container applies the safe drawing insets to content. Pane-contained windows continue to preserve visible parent context and gutters.

| Before | After |
| --- | --- |
| `review-before.png` | `review-after.png` |
| `areas-before.png` | `areas-after.png` |

All four captures are fresh live images from the open Samsung Galaxy Fold. The after images came from the exact signed release installed at `2026-08-22 16:38:40 America/Toronto`.
