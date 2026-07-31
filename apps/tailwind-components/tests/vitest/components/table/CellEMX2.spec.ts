import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import CellEMX2 from "../../../../app/components/table/CellEMX2.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

describe("table/CellEMX2.vue ENUM/MODULE column types", () => {
  it("renders a scalar ENUM value as its string value, not blank", () => {
    const metadata: IColumn = {
      id: "status",
      label: "Status",
      columnType: "ENUM",
    };

    const wrapper = mount(CellEMX2, {
      props: { metadata, data: "active" },
    });

    expect(wrapper.text()).toBe("active");
  });

  it("renders a scalar MODULE value as its string value, not blank", () => {
    const metadata: IColumn = {
      id: "experimentType",
      label: "Experiment type",
      columnType: "MODULE",
    };

    const wrapper = mount(CellEMX2, {
      props: { metadata, data: "RNA" },
    });

    expect(wrapper.text()).toBe("RNA");
  });

  it("renders an ENUM_ARRAY value as a joined string list, not the type-name fallback", () => {
    const metadata: IColumn = {
      id: "statuses",
      label: "Statuses",
      columnType: "ENUM_ARRAY",
    };

    const wrapper = mount(CellEMX2, {
      props: { metadata, data: ["active", "pending"] },
    });

    expect(wrapper.text()).toContain("active");
    expect(wrapper.text()).toContain("pending");
    expect(wrapper.text()).not.toContain("ENUM");
  });

  it("renders a MODULE_ARRAY value as a joined string list, not the type-name fallback", () => {
    const metadata: IColumn = {
      id: "diseaseGroup",
      label: "Disease group",
      columnType: "MODULE_ARRAY",
    };

    const wrapper = mount(CellEMX2, {
      props: { metadata, data: ["CockayneSyndrome"] },
    });

    expect(wrapper.text()).toContain("CockayneSyndrome");
    expect(wrapper.text()).not.toContain("MODULE");
  });
});
