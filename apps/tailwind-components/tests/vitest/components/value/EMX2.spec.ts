import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

describe("value/EMX2.vue MODULE column type", () => {
  it("renders a MODULE value as its string value via ValueString, not the type-name fallback", () => {
    const metadata: IColumn = {
      id: "experimentType",
      label: "Experiment type",
      columnType: "MODULE",
    };

    const wrapper = mount(ValueEMX2, {
      props: {
        metadata,
        data: "MRI",
      },
    });

    expect(wrapper.text()).toBe("MRI");
    expect(wrapper.text()).not.toBe("MODULE");
  });

  it("renders an ENUM value as its string value via ValueString, not the type-name fallback", () => {
    const metadata: IColumn = {
      id: "status",
      label: "Status",
      columnType: "ENUM",
    };

    const wrapper = mount(ValueEMX2, {
      props: {
        metadata,
        data: "active",
      },
    });

    expect(wrapper.text()).toBe("active");
    expect(wrapper.text()).not.toBe("ENUM");
  });

  it("renders a MODULE_ARRAY value as a joined string list, not the type-name fallback", () => {
    const metadata: IColumn = {
      id: "diseaseGroup",
      label: "Disease group",
      columnType: "MODULE_ARRAY",
    };

    const wrapper = mount(ValueEMX2, {
      props: {
        metadata,
        data: ["CockayneSyndrome"],
      },
    });

    expect(wrapper.text()).toContain("CockayneSyndrome");
    expect(wrapper.text()).not.toContain("MODULE");
  });

  it("renders an ENUM_ARRAY value as a joined string list, not the type-name fallback", () => {
    const metadata: IColumn = {
      id: "statuses",
      label: "Statuses",
      columnType: "ENUM_ARRAY",
    };

    const wrapper = mount(ValueEMX2, {
      props: {
        metadata,
        data: ["active", "pending"],
      },
    });

    expect(wrapper.text()).toContain("active");
    expect(wrapper.text()).toContain("pending");
    expect(wrapper.text()).not.toContain("ENUM");
  });
});
