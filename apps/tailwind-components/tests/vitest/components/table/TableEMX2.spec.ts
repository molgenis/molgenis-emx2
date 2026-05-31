import { describe, it, expect } from "vitest";
import { resolveEmptyRowsLabel } from "../../../../app/components/table/TableEMX2.vue";

describe("resolveEmptyRowsLabel", () => {
  it("returns 'No records found' when no filters or search are active", () => {
    expect(resolveEmptyRowsLabel(false)).toBe("No records found");
  });

  it("returns 'No data matched the filters' when filters are active", () => {
    expect(resolveEmptyRowsLabel(true)).toBe("No data matched the filters");
  });
});
