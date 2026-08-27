import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataLinks from "../../../../app/components/display/DataLinks.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../../../../app/types/display";

const ageColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

const resolved: ResolvedDisplay = {
  layout: "LINKS",
  titleTemplate: "${name}",
  detailColumns: [ageColumn],
};

describe("DataLinks.vue", () => {
  it("renders one anchor per record, with title text only", () => {
    const wrapper = mount(DataLinks, {
      props: {
        rows,
        resolved,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const items = wrapper.findAll("li");
    expect(items.length).toBe(2);

    const anchors = wrapper.findAll("a");
    expect(anchors.length).toBe(2);
    expect(anchors[0].attributes("href")).toBe("/records/Tweety");
    expect(anchors[0].text()).toBe("Tweety");
    expect(anchors[1].attributes("href")).toBe("/records/Sylvester");
    expect(anchors[1].text()).toBe("Sylvester");
    expect(anchors[0].classes()).toContain("underline");

    expect(wrapper.text()).not.toContain("3");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataLinks, {
      props: { rows, resolved },
    });

    expect(wrapper.find("a").exists()).toBe(false);
    const items = wrapper.findAll("li");
    expect(items[0].text()).toBe("Tweety");
  });
});
