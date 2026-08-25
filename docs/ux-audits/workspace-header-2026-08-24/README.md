# Workspace header implementation evidence

This directory records the live-app evidence used to verify the workspace-header redesign. The `fresh/` captures came from the final debug build running on a disposable Android emulator; older repository screenshots were not used as design evidence.

The final expanded-layout captures are:

- `fresh/final-wide-tracks.png` — first-class Tracks workspace and Track Overview support pane.
- `fresh/final-wide-activity.png` — cross-Track chronological Activity destination.
- `fresh/final-wide-insights.png` — cross-Track Insights and automation-health destination.
- `fresh/final-wide-gym.png` — global Gym workspace with the shared header/action geometry and left-side support pane.

The matching XML files document the semantic hierarchy and control bounds used by the automated geometry checks. System bars remain visible in these internal QA captures so the complete expanded-window composition can be inspected.
