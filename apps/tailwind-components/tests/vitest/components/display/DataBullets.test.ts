import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataBullets from "../../../../app/components/display/DataBullets.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../../../app/types/display";

const nameColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
  key: 1,
};

const columns: IColumn[] = [nameColumn];

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

const displayConfig: DisplayConfig = { layout: "BULLETS" };

describe("DataBullets.vue", () => {
  it("renders one bulleted anchor per record, with title text only", () => {
    const wrapper = mount(DataBullets, {
      props: {
        rows,
        columns,
        displayConfig,
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
    const wrapper = mount(DataBullets, {
      props: { rows, columns, displayConfig },
    });

    expect(wrapper.find("a").exists()).toBe(false);
    const items = wrapper.findAll("li");
    expect(items[0].text()).toBe("Tweety");
  });

  it("renders from rows and displayConfig alone, with no columns prop, when titleTemplate is given explicitly", () => {
    const wrapper = mount(DataBullets, {
      props: {
        rows,
        displayConfig: { layout: "BULLETS", titleTemplate: "${name}" },
      },
    });

    const items = wrapper.findAll("li");
    expect(items[0].text()).toBe("Tweety");
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(DataBullets, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        displayConfig: { layout: "BULLETS", titleTemplate: "" },
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const anchor = wrapper.find("li a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });
});
