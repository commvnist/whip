# Compact equal activity controls

Source `2bdf490639a7a81f29730bb6389b63abe443b590` replaces the rejected 80dp reserve with each group's tallest natural label height and a **48dp minimum**. Siblings retain equal width and height; normal phone controls share two leading columns. Padding remains 12dp horizontal/8dp vertical at every text scale. Long labels grow only their group; large text uses the existing width-based wrapping rule.

The consultant rejected the global empty-space reserve and reviewed actual normal Habit, Goal and Home captures plus current native200% card, long-inspector and correction images. Normal routine controls are142×48dp (40% less height); the unchanged synthetic Home Habit card is138dp instead of170dp, with actions35% rather than47% of its height. Both Home Habit cards now fit without scrolling. Labels, checkbox spacing, hierarchy, status and navigation remain readable and stable. Existing card/status gaps were reviewed; changing those was unnecessary to resolve the oversized action block.

[Before/after design and critique](../design-density/README.md), [focused normal/200% results](../qa/phone-content-height/README.md), [compile/lint and frozen provenance](implementation/README.md).

Exactly the two affected native methods passed at1080×2520/d480 (360dp): normal90.581s;200%131.243s. They verify48dp normal height, local equality and paired rows, full glyphs, long labels, saved duration/numeric/Goal progress and completion Cancel/reachability. Readiness, source guards and the single debug/test package/lint batch passed; lint has zero errors. Source/app/test were frozen throughout execution. Earlier valid passes remain retained with their source identities; no full suite rerun.

Limits: Home's unchanged fixture configuration contains Habits only; Goal is directly verified in its tab. The correction does not claim TalkBack speech or a broad locale/device matrix. Native card context can scroll at200%; essential action labels remain complete. Personal attached images are excluded from repository evidence. Phone release verification is separate.
