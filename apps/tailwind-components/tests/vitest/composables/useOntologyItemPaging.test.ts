import { ref } from "vue";
import { describe, expect, it } from "vitest";
import { useOntologyItemPaging } from "../../../app/composables/useOntologyItemPaging";

describe("useOntologyItemPaging", () => {
  it("hides items past maxItems and shows a control", () => {
    const { showControl, isHidden, controlLabel } = useOntologyItemPaging(
      ref(12),
      ref(5),
      ref(3)
    );

    expect(showControl.value).toBe(true);
    expect(isHidden(4)).toBe(false);
    expect(isHidden(5)).toBe(true);
    expect(controlLabel.value).toBe("Show more");
  });

  it("toggle reveals itemStep more items without exceeding the count", () => {
    const { isHidden, toggle } = useOntologyItemPaging(ref(12), ref(5), ref(3));

    toggle();

    expect(isHidden(7)).toBe(false);
    expect(isHidden(8)).toBe(true);
  });

  it("flips to show less once fully expanded, and resets to maxItems on toggle", () => {
    const { isFullyExpanded, isHidden, controlLabel, toggle } =
      useOntologyItemPaging(ref(5), ref(3), ref(2));

    toggle(); // 3 + 2 = 5, fully expanded
    expect(isFullyExpanded.value).toBe(true);
    expect(controlLabel.value).toBe("Show less");

    toggle(); // resets to maxItems
    expect(isFullyExpanded.value).toBe(false);
    expect(isHidden(3)).toBe(true);
    expect(controlLabel.value).toBe("Show more");
  });

  it("never hides anything and never shows a control when maxItems is unset", () => {
    const { showControl, isFullyExpanded, isHidden } = useOntologyItemPaging(
      ref(20),
      ref(undefined),
      ref(5)
    );

    expect(showControl.value).toBe(false);
    expect(isFullyExpanded.value).toBe(true);
    expect(isHidden(19)).toBe(false);
  });
});
