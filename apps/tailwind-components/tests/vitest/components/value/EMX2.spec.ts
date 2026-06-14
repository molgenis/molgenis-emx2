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
});
