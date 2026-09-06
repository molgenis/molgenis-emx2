import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataCards from "../../../../app/components/display/DataCards.vue";
import DataPairs from "../../../../app/components/display/DataPairs.vue";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../../../app/types/display";

const nameColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
  key: 1,
};

const ageColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const bioColumn: IColumn = {
  id: "bio",
  label: "Bio",
  columnType: "TEXT",
};

// Title = name (the key column), description = bio (the only TEXT column),
// detail = age (everything left over): all three slots come from
// resolveDisplay's own defaults, not from an override.
const columns: IColumn[] = [nameColumn, ageColumn, bioColumn];

const rows: IRow[] = [
  { name: "Tweety", age: 3, bio: "A bird named Tweety" },
  { name: "Sylvester", age: 5, bio: "A bird named Sylvester" },
];

const displayConfig: DisplayConfig = { layout: "CARDS" };

describe("DataCards.vue", () => {
  it("renders one card per record, with title and description, and passes its detail columns to DataPairs", () => {
    const wrapper = mount(DataCards, {
      props: { rows, columns, displayConfig },
    });

    const cards = wrapper.findAll("li");
    expect(cards.length).toBe(2);
    expect(cards[0].text()).toContain("Tweety");
    expect(cards[0].text()).toContain("A bird named Tweety");

    const pairsComponents = wrapper.findAllComponents(DataPairs);
    expect(pairsComponents.length).toBe(2);
    expect(pairsComponents[0].props("columns")).toEqual([ageColumn]);
    expect(pairsComponents[0].props("row")).toEqual(rows[0]);
  });

  it("renders from rows and displayConfig alone, with no columns prop, when titleTemplate is given explicitly", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows,
        displayConfig: { layout: "CARDS", titleTemplate: "${name}" },
      },
    });

    expect(wrapper.findAll("li")[0].text()).toContain("Tweety");
  });

  it("lays cards out as a two-column grid", () => {
    const wrapper = mount(DataCards, {
      props: { rows, columns, displayConfig },
    });
    expect(wrapper.find("ul").classes()).toContain("lg:grid-cols-2");
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows,
        columns,
        displayConfig,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const firstAnchor = wrapper.find("li a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataCards, {
      props: { rows, columns, displayConfig },
    });

    expect(wrapper.find("li a").exists()).toBe(false);
  });

  it("renders the logo image with an empty alt when logoColumn resolves to a file value", () => {
    const logoColumn: IColumn = {
      id: "logo",
      label: "Logo",
      columnType: "FILE",
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
        columns: [...columns, logoColumn],
        displayConfig: { layout: "CARDS", logoColumnId: "logo" },
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
    const wrapper = mount(DataCards, {
      props: {
        rows: [{ name: "Tweety", age: 3, logo: "not-a-file" }],
        columns: [...columns, logoColumn],
        displayConfig: { layout: "CARDS", logoColumnId: "logo" },
      },
    });

    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        displayConfig: { layout: "CARDS", titleTemplate: "" },
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

  it("borders every card with plain `border`, never `border-theme`, and drops the left border on the even column", () => {
    const wrapper = mount(DataCards, {
      props: { rows, columns, displayConfig },
    });
    const items = wrapper.findAll("li");
    expect(items[0].classes()).toContain("border");
    expect(items[0].classes()).not.toContain("border-theme");
    expect(items[0].classes()).toContain("lg:even:border-l-0");
    expect(items[1].classes()).toContain("lg:even:border-l-0");
  });

  it("forwards maxLines, renderLimit, truncate and hideListSeparator to DataPairs unchanged", () => {
    // The forwarding from DataPairs down to value/EMX2.vue is DataPairs'
    // own responsibility, covered in DataPairs.test.ts; this only checks
    // the hop DataCards itself owns.
    const wrapper = mount(DataCards, {
      props: {
        rows,
        columns,
        displayConfig,
        maxLines: 2,
        renderLimit: 5,
        truncate: false,
        hideListSeparator: true,
      },
    });

    const pairsComponent = wrapper.findComponent(DataPairs);
    expect(pairsComponent.props("maxLines")).toBe(2);
    expect(pairsComponent.props("renderLimit")).toBe(5);
    expect(pairsComponent.props("truncate")).toBe(false);
    expect(pairsComponent.props("hideListSeparator")).toBe(true);
  });

  it("renders the description through value/EMX2.vue, forwarding maxLines and truncate unchanged so the clamp survives", () => {
    // ValueText.vue wraps ShowMore with these two props, so asserting them
    // on ValueEMX2 is what proves the clamp still applies.
    const wrapper = mount(DataCards, {
      props: {
        rows,
        columns,
        displayConfig,
        maxLines: 7,
        truncate: false,
      },
    });

    const descriptionCell = wrapper.findComponent(ValueEMX2);
    expect(descriptionCell.props("metadata")).toEqual(bioColumn);
    expect(descriptionCell.props("data")).toBe("A bird named Tweety");
    expect(descriptionCell.props("maxLines")).toBe(7);
    expect(descriptionCell.props("truncate")).toBe(false);
  });

  it("renders no subtitle element when subtitleTemplate is not set", () => {
    const wrapper = mount(DataCards, {
      props: { rows, columns, displayConfig },
    });

    expect(wrapper.find("li span.mt-1\\.5").exists()).toBe(false);
  });

  it("renders the subtitle beside the title when subtitleTemplate is set", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows,
        displayConfig: {
          layout: "CARDS",
          titleTemplate: "${age}",
          subtitleTemplate: "${name}",
        },
      },
    });

    const card = wrapper.findAll("li")[0];
    expect(card.find(".font-bold").text()).toBe("3");
    const subtitle = card.find("span.mt-1\\.5");
    expect(subtitle.exists()).toBe(true);
    expect(subtitle.text()).toBe("Tweety");
  });

  it("promotes the subtitle into the title slot, and renders no subtitle element, when the title is empty", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows,
        displayConfig: {
          layout: "CARDS",
          titleTemplate: "",
          subtitleTemplate: "${name}",
        },
      },
    });

    const card = wrapper.findAll("li")[0];
    expect(card.find(".font-bold").text()).toBe("Tweety");
    expect(card.find("span.mt-1\\.5").exists()).toBe(false);
  });
});
