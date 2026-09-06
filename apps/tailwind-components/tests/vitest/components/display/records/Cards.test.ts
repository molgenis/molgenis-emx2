import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Cards from "../../../../../app/components/display/records/Cards.vue";
import Pairs from "../../../../../app/components/display/records/Pairs.vue";
import ValueEMX2 from "../../../../../app/components/value/EMX2.vue";
import type { IColumn, IRow } from "../../../../../../metadata-utils/src/types";

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

const rows: IRow[] = [
  { name: "Tweety", age: 3, bio: "A bird named Tweety" },
  { name: "Sylvester", age: 5, bio: "A bird named Sylvester" },
];

describe("records/Cards.vue", () => {
  it("renders one card per record, with title and description, and passes its detail columns to Pairs", () => {
    const wrapper = mount(Cards, {
      props: {
        rows,
        titleTemplate: "${name}",
        descriptionColumn: bioColumn,
        detailColumns: [ageColumn],
      },
    });

    const cards = wrapper.findAll("li");
    expect(cards.length).toBe(2);
    expect(cards[0].text()).toContain("Tweety");
    expect(cards[0].text()).toContain("A bird named Tweety");

    const pairsComponents = wrapper.findAllComponents(Pairs);
    expect(pairsComponents.length).toBe(2);
    expect(pairsComponents[0].props("columns")).toEqual([ageColumn]);
    expect(pairsComponents[0].props("row")).toEqual(rows[0]);
  });

  it("lays cards out as a two-column grid", () => {
    const wrapper = mount(Cards, {
      props: { rows, titleTemplate: "${name}" },
    });
    expect(wrapper.find("ul").classes()).toContain("lg:grid-cols-2");
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(Cards, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const firstAnchor = wrapper.find("li a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(Cards, {
      props: { rows, titleTemplate: "${name}" },
    });

    expect(wrapper.find("li a").exists()).toBe(false);
  });

  it("renders the logo image with an empty alt when logoColumn resolves to a file value", () => {
    const logoColumn: IColumn = {
      id: "logo",
      label: "Logo",
      columnType: "FILE",
    };
    const wrapper = mount(Cards, {
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
        titleTemplate: "${name}",
        logoColumn,
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
    const wrapper = mount(Cards, {
      props: {
        rows: [{ name: "Tweety", age: 3, logo: "not-a-file" }],
        titleTemplate: "${name}",
        logoColumn,
      },
    });

    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(Cards, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        titleTemplate: "",
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
    const wrapper = mount(Cards, {
      props: { rows, titleTemplate: "${name}" },
    });
    const items = wrapper.findAll("li");
    expect(items[0].classes()).toContain("border");
    expect(items[0].classes()).not.toContain("border-theme");
    expect(items[0].classes()).toContain("lg:even:border-l-0");
    expect(items[1].classes()).toContain("lg:even:border-l-0");
  });

  it("renders the description through value/EMX2.vue", () => {
    const wrapper = mount(Cards, {
      props: {
        rows,
        titleTemplate: "${name}",
        descriptionColumn: bioColumn,
      },
    });

    const descriptionCell = wrapper.findComponent(ValueEMX2);
    expect(descriptionCell.props("metadata")).toEqual(bioColumn);
    expect(descriptionCell.props("data")).toBe("A bird named Tweety");
  });

  it("renders no subtitle element when subtitleTemplate is not set", () => {
    const wrapper = mount(Cards, {
      props: { rows, titleTemplate: "${name}" },
    });

    expect(wrapper.find("li span.mt-1\\.5").exists()).toBe(false);
  });

  it("renders the subtitle beside the title when subtitleTemplate is set", () => {
    const wrapper = mount(Cards, {
      props: {
        rows,
        titleTemplate: "${age}",
        subtitleTemplate: "${name}",
      },
    });

    const card = wrapper.findAll("li")[0];
    expect(card.find(".font-bold").text()).toBe("3");
    const subtitle = card.find("span.mt-1\\.5");
    expect(subtitle.exists()).toBe(true);
    expect(subtitle.text()).toBe("Tweety");
  });

  it("promotes the subtitle into the title slot, and renders no subtitle element, when the title is empty", () => {
    const wrapper = mount(Cards, {
      props: {
        rows,
        titleTemplate: "",
        subtitleTemplate: "${name}",
      },
    });

    const card = wrapper.findAll("li")[0];
    expect(card.find(".font-bold").text()).toBe("Tweety");
    expect(card.find("span.mt-1\\.5").exists()).toBe(false);
  });
});
