import { ref } from "vue";
import { describe, expect, it } from "vitest";
import { useOntologyItemPaging } from "../../../app/composables/useOntologyItemPaging";

describe("useOntologyItemPaging", () => {
  it("hides items past maxItems and shows a control", () => {
    const paging = useOntologyItemPaging(ref(12), ref(5), ref(3));

    expect(paging.showControl).toBe(true);
    expect(paging.isHidden(4)).toBe(false);
    expect(paging.isHidden(5)).toBe(true);
    expect(paging.controlLabel).toBe("Show more");
  });

  it("toggle reveals itemStep more items without exceeding the count", () => {
    const paging = useOntologyItemPaging(ref(12), ref(5), ref(3));

    paging.toggle();

    expect(paging.isHidden(7)).toBe(false);
    expect(paging.isHidden(8)).toBe(true);
  });

  it("flips to show less once fully expanded, and resets to maxItems on toggle", () => {
    const paging = useOntologyItemPaging(ref(5), ref(3), ref(2));

    paging.toggle(); // 3 + 2 = 5, fully expanded
    expect(paging.isFullyExpanded).toBe(true);
    expect(paging.controlLabel).toBe("Show less");

    paging.toggle(); // resets to maxItems
    expect(paging.isFullyExpanded).toBe(false);
    expect(paging.isHidden(3)).toBe(true);
    expect(paging.controlLabel).toBe("Show more");
  });

  it("never hides anything and never shows a control when maxItems is unset", () => {
    const paging = useOntologyItemPaging(ref(20), ref(undefined), ref(5));

    expect(paging.showControl).toBe(false);
    expect(paging.isFullyExpanded).toBe(true);
    expect(paging.isHidden(19)).toBe(false);
  });
});
