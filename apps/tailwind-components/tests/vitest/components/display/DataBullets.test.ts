import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataBullets from "../../../../app/components/display/DataBullets.vue";
import ShowMore from "../../../../app/components/ShowMore.vue";
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
  layout: "BULLETS",
  titleTemplate: "${name}",
  detailColumns: [ageColumn],
};

describe("DataBullets.vue", () => {
  it("renders one bulleted anchor per record, with title text only", () => {
    const wrapper = mount(DataBullets, {
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
    const wrapper = mount(DataBullets, {
      props: { rows, resolved },
    });

    expect(wrapper.find("a").exists()).toBe(false);
    const items = wrapper.findAll("li");
    expect(items[0].text()).toBe("Tweety");
  });

  it("renders no title text when resolved.titleTemplate is empty", () => {
    const emptyResolved: ResolvedDisplay = {
      layout: "BULLETS",
      titleTemplate: "",
      detailColumns: [],
    };
    const wrapper = mount(DataBullets, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        resolved: emptyResolved,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const anchor = wrapper.find("li a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });

  it("renders only renderLimit rows and shows the control while more remain", () => {
    const manyRows: IRow[] = [
      { name: "A" },
      { name: "B" },
      { name: "C" },
      { name: "D" },
    ];
    const wrapper = mount(DataBullets, {
      props: {
        rows: manyRows,
        resolved,
        renderLimit: 2,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const items = wrapper.findAll("li");
    expect(items.length).toBe(2);
    expect(items[0].text()).toBe("A");
    expect(items[1].text()).toBe("B");
    expect(wrapper.text()).not.toContain("C");

    expect(wrapper.find("button").exists()).toBe(true);
  });

  it("renders more rows once the control fires", async () => {
    const manyRows: IRow[] = [
      { name: "A" },
      { name: "B" },
      { name: "C" },
      { name: "D" },
    ];
    const wrapper = mount(DataBullets, {
      props: {
        rows: manyRows,
        resolved,
        renderLimit: 2,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    await wrapper.find("button").trigger("click");

    const items = wrapper.findAll("li");
    expect(items.length).toBe(4);
    expect(items[3].text()).toBe("D");
  });

  it("renders no control when there are fewer rows than renderLimit", () => {
    const wrapper = mount(DataBullets, {
      props: { rows, resolved, renderLimit: 10 },
    });

    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("passes truncate true and an effectively unbounded maxLines to ShowMore, so hasMore alone drives the control rather than a line clamp", () => {
    // jsdom performs no layout, so it cannot prove the line clamp never
    // visually engages; this pins the props that keep the two notions of
    // "more" independent in a real browser. ShowMore's own control only
    // ever appears while truncate is true, so truncate cannot be false
    // here; an effectively unbounded maxLines keeps the clamp from ever
    // reporting overflow on its own, leaving hasMore as the only real
    // trigger.
    const wrapper = mount(DataBullets, {
      props: { rows, resolved, renderLimit: 1 },
    });

    const showMore = wrapper.findComponent(ShowMore);
    expect(showMore.props("truncate")).toBe(true);
    expect(showMore.props("maxLines")).toBeGreaterThan(1_000_000);
    expect(showMore.props("hasMore")).toBe(true);
  });
});
