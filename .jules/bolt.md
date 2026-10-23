
## 2024-05-24 - Precomputing Draw Loop Allocations/Calculations in Android UI
**Learning:** In Android custom view development (like `Keyboard2View`), repeatedly calculating contrast ratios and color adjustments (e.g. `Theme.getContrastColor`, `Theme.adjustLight`) during the `onDraw` pass per key causes significant rendering overhead. Variables driven by static Theme configurations should be cached and computed once during the theme instantiation.
**Action:** When inspecting Android view rendering code, look for computations inside `onDraw` or helper functions called by `onDraw`. Move heavy math or object creation (like Color manipulation) to the class constructor or initialization block.
