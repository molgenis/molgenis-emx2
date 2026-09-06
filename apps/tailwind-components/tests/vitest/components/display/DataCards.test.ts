import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataCards from "../../../../app/components/display/DataCards.vue";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
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
  layout: "CARDS",
  titleTemplate: "${name}",
  descriptionTemplate: "A bird named ${name}",
  detailColumns: [ageColumn],
};

describe("DataCards.vue", () => {
  it("renders one card per record, with title, description and detail columns", () => {
    const wrapper = mount(DataCards, {
      props: { rows, resolved },
    });

    const cards = wrapper.findAll("li");
    expect(cards.length).toBe(2);
    expect(cards[0].text()).toContain("Tweety");
    expect(cards[0].text()).toContain("A bird named Tweety");
    expect(cards[0].text()).toContain("Age");

    const cellComponents = wrapper.findAllComponents(ValueEMX2);
    expect(cellComponents.length).toBe(2);
    expect(cellComponents[0].props("metadata")).toEqual(ageColumn);
    expect(cellComponents[0].props("data")).toBe(3);
  });

  it("renders a grid when columnCount is 2 and full-width rows when columnCount is 1", () => {
    const gridWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 2 },
    });
    expect(gridWrapper.find("ul").classes()).toContain("lg:grid-cols-2");

    const listWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 1 },
    });
    expect(listWrapper.find("ul").classes()).not.toContain("lg:grid-cols-2");
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(DataCards, {
      props: { rows, resolved, linkTo: (row: IRow) => `/records/${row.name}` },
    });

    const firstAnchor = wrapper.find("li a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataCards, {
      props: { rows, resolved },
    });

    expect(wrapper.find("li a").exists()).toBe(false);
  });

  it("renders the logo image with an empty alt when logoColumn resolves to a file value", () => {
    const logoColumn: IColumn = {
      id: "logo",
      label: "Logo",
      columnType: "FILE",
    };
    const resolvedWithLogo: ResolvedDisplay = {
      ...resolved,
      logoColumn,
    };
    const wrapper = mount(DataCards, {
      props: {
        rows: [
          {
            name: "Tweety",
            age: 3,
            logo: {
              id: "f1",
              size: 100,
              filename: "tweety.png",
              extension: "png",
              url: "https://example.org/tweety.png",
            },
          },
        ],
        resolved: resolvedWithLogo,
      },
    });

    const img = wrapper.find("img");
    expect(img.attributes("src")).toBe("https://example.org/tweety.png");
    expect(img.attributes("alt")).toBe("");
  });

  it("renders no logo when logoColumn does not resolve to a file value", () => {
    const logoColumn: IColumn = {
      id: "logo",
      label: "Logo",
      columnType: "STRING",
    };
    const resolvedWithLogo: ResolvedDisplay = {
      ...resolved,
      logoColumn,
    };
    const wrapper = mount(DataCards, {
      props: {
        rows: [{ name: "Tweety", age: 3, logo: "not-a-file" }],
        resolved: resolvedWithLogo,
      },
    });

    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("renders no title text when resolved.titleTemplate is empty", () => {
    const emptyResolved: ResolvedDisplay = {
      layout: "CARDS",
      titleTemplate: "",
      detailColumns: [],
    };
    const wrapper = mount(DataCards, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        resolved: emptyResolved,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const card = wrapper.find("li");
    const anchor = card.find("a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(card.text()).not.toContain("LL");
    expect(card.text()).not.toContain("2006");
  });

  it("borders every card with plain `border`, never `border-theme`, whose --border-width-theme is 0 in four themes", () => {
    const listWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 1 },
    });
    const listItems = listWrapper.findAll("li");
    expect(listItems[0].classes()).toContain("border");
    expect(listItems[0].classes()).not.toContain("border-theme");
    expect(listItems[0].classes()).not.toContain("lg:even:border-l-0");

    const gridWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 2 },
    });
    const gridItems = gridWrapper.findAll("li");
    expect(gridItems[0].classes()).toContain("border");
    expect(gridItems[0].classes()).not.toContain("border-theme");
    expect(gridItems[0].classes()).toContain("lg:even:border-l-0");
    expect(gridItems[1].classes()).toContain("lg:even:border-l-0");
  });

  it("wraps detail pairs by their own min-width in LIST density, not by a viewport breakpoint, and keeps the stacked list in CARDS density", () => {
    const listWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 1 },
    });
    expect(listWrapper.find("dl").classes()).toContain("flex-wrap");
    expect(listWrapper.find("dl > div").classes()).toContain("min-w-[160px]");

    const gridWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 2 },
    });
    expect(gridWrapper.find("dl").classes()).not.toContain("flex-wrap");
    expect(gridWrapper.find("dl > div").classes()).not.toContain(
      "min-w-[160px]"
    );
  });

  it("forwards maxLines, renderLimit, truncate and hideListSeparator to value/EMX2.vue unchanged", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows,
        resolved,
        maxLines: 2,
        renderLimit: 5,
        truncate: false,
        hideListSeparator: true,
      },
    });

    const cellComponent = wrapper.findComponent(ValueEMX2);
    expect(cellComponent.props("maxLines")).toBe(2);
    expect(cellComponent.props("renderLimit")).toBe(5);
    expect(cellComponent.props("truncate")).toBe(false);
    expect(cellComponent.props("hideListSeparator")).toBe(true);
  });
});
