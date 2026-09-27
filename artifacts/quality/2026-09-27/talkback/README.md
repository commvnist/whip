# Focused TalkBack speech check

API 34 Pixel emulator `emulator-5554`, Android Accessibility Suite TalkBack 14.2, English, with **Display speech output** enabled. Android's [accessibility testing guidance](https://developer.android.com/guide/topics/ui/accessibility/testing) identifies this setting as a way to inspect TalkBack announcements. The emulator was started without host audio, so these are retained screenshots of TalkBack's displayed speech output, not an audio file.

The native probe requested accessibility focus on the actual shared node after rendering; each ordinary test also passed without the probe argument. The visible green outline identifies the focused control.

| Representative focus | Displayed TalkBack output | Evidence |
| --- | --- | --- |
| Shared `WhipGroupHeading("Areas")` | “Areas. Heading” | [heading.png](heading.png) |
| Shared warning Card, before correction | “Warning” | [warning-before.png](warning-before.png) |
| Same warning Card, after descendant merge | “Warning. Setting Saved with Warnings. Reminder permission was denied.” | [warning-after.png](warning-after.png) |
| Busy disabled shared destructive confirmation | “Delete Permanently. Button. disabled” | [disabled-delete.png](disabled-delete.png) |

The warning correction is in `WhipNoticeCard`. Its semantic regression checks that the single status node carries both title and message, while an optional Retry action remains separately clickable. The disabled action was focused at its actual button node, not at its text child. The final normal four-method Android batch passed. This focused check does not claim a full TalkBack traversal of every destination or speech behavior on other devices/languages.
