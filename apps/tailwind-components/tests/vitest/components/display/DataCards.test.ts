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
    expect(gridWrapper.find("ul").classes()).toContain("grid-cols-2");

    const listWrapper = mount(DataCards, {
      props: { rows, resolved, columnCount: 1 },
    });
    expect(listWrapper.find("ul").classes()).not.toContain("grid-cols-2");
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
});
