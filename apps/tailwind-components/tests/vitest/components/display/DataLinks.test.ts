import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataLinks from "../../../../app/components/display/DataLinks.vue";
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

const displayConfig: DisplayConfig = { layout: "LINKS" };

describe("DataLinks.vue", () => {
  it("renders record titles comma-separated on one line, each its own anchor, no trailing comma", () => {
    const wrapper = mount(DataLinks, {
      props: {
        rows,
        columns,
        displayConfig,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const anchors = wrapper.findAll("a");
    expect(anchors.length).toBe(2);
    expect(anchors[0].attributes("href")).toBe("/records/Tweety");
    expect(anchors[0].text()).toBe("Tweety");
    expect(anchors[1].attributes("href")).toBe("/records/Sylvester");
    expect(anchors[1].text()).toBe("Sylvester");
    expect(anchors[0].classes()).toContain("underline");

    expect(wrapper.text().replace(/\s+/g, " ").trim()).toBe(
      "Tweety, Sylvester"
    );
    expect(wrapper.text()).not.toContain("3");
  });

  it("renders plain comma-separated text with no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataLinks, {
      props: { rows, columns, displayConfig },
    });

    expect(wrapper.find("a").exists()).toBe(false);
    expect(wrapper.text().replace(/\s+/g, " ").trim()).toBe(
      "Tweety, Sylvester"
    );
  });

  it("renders from rows and displayConfig alone, with no columns prop, when titleTemplate is given explicitly", () => {
    const wrapper = mount(DataLinks, {
      props: {
        rows,
        displayConfig: { layout: "LINKS", titleTemplate: "${name}" },
      },
    });

    expect(wrapper.text().replace(/\s+/g, " ").trim()).toBe(
      "Tweety, Sylvester"
    );
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(DataLinks, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        displayConfig: { layout: "LINKS", titleTemplate: "" },
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const anchor = wrapper.find("a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });
});
