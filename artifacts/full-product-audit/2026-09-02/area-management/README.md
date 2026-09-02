# Area management interaction audit

Device: disposable API 34 emulator, 1080 x 2400 at 420 dpi.

This evidence verifies the remediated Area-management hierarchy and lifecycle flow:

- `area-manager-final.png` / `.xml`: the manager exposes creation, search, reordering, active Areas, and row actions in a full-height surface.
- `area-manager-detail.png` / `.xml`: an active Area has distinct identity, organization, lifecycle, and destructive controls.
- `archive-confirm.png` / `.xml`: archiving requires an explicit confirmation and explains picker behavior.
- `archive-success.png` / `.xml`: the committed result remains in context, reports success with Undo, labels the Area archived, and offers the correct `Restore Area` action.

Automated coverage separately verifies that failed mutations retain their dialog and draft, archived-name creation restores instead of selecting an inaccessible Area, archived search matches appear without manual disclosure, and repository-backed mutations publish exact outcomes.
