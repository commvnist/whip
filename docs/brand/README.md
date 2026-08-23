# Whip brand mark

Whip uses a soft, continuous W monogram designed to remain clear in the
Android launcher, Samsung taskbar, foldable navigation rail, and app bar.

## Palette

- Near black: `#090909`
- Warm white: `#F5F3EA`

The shipping artwork is vector-native. `whip-monogram-concept.png` is the visual
exploration retained for design provenance; Android renders the deterministic
resources in `app/src/main/res/drawable`. `whip-app-icon.jpg` is the requested
512×512 RGB repository export. `whip-app-icon-play.png` is the matching 512×512
32-bit RGBA export for the Google Play store listing.

Google Play listing assets are kept here with the source brand material:

- `whip-app-icon-play.png` — 512×512 RGBA Play Store app icon.
- `whip-app-icon.jpg` — 512×512 RGB/JPEG repository export of the same icon.
- `whip-feature-graphic-play.png` — 1024×500 RGB Play Store feature graphic.
- `whip-feature-graphic.jpg` — 1024×500 RGB/JPEG feature-graphic export.

Regenerate all four exports from the shipping geometry by running
`java scripts/RenderBrandIcon.java` from the repository root.

## Final generation prompt

The original concept was edited with OpenAI's built-in image generator using:

> Use case: precise-object-edit
> Asset type: Whip Android app icon and brand-mark concept
> Input images: Image 1: edit target, the original wavy Whip W monogram
> Primary request: remove only the yellow circular dot
> Constraints: change only the yellow dot and fill the removed area seamlessly
> with the existing black background; preserve the wavy warm-white W exactly,
> including its silhouette, proportions, curves, overlap, small black separation
> line, placement, scale, colors, and padding; keep the black background unchanged;
> do not add any accent, text, texture, shadow, border, or other element
