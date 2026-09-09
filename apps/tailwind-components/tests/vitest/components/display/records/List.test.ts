import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import RecordsList from "../../../../../app/components/display/records/List.vue";
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

// NuxtLink resolves to vue-router's RouterLink, which the plain test
// environment has none of, so it must be stubbed down to a real <a> to
// assert on href/text the way the app's own router would render it.
const global = {
  stubs: {
    NuxtLink: {
      props: ["to"],
      template: '<a :href="to"><slot /></a>',
    },
  },
};

describe("records/List.vue", () => {
  it("renders one row per record, with title and description, and passes its detail columns to Pairs in the wide shape", () => {
    const wrapper = mount(RecordsList, {
      props: {
        rows,
        titleTemplate: "${name}",
        descriptionColumn: bioColumn,
        detailColumns: [ageColumn],
      },
    });

    const items = wrapper.findAll("li");
    expect(items.length).toBe(2);
    expect(items[0].text()).toContain("Tweety");
    expect(items[0].text()).toContain("A bird named Tweety");

    const pairsComponents = wrapper.findAllComponents(Pairs);
    expect(pairsComponents.length).toBe(2);
    expect(pairsComponents[0].props("columns")).toEqual([ageColumn]);
    expect(pairsComponents[0].props("row")).toEqual(rows[0]);
    expect(pairsComponents[0].props("wide")).toBe(true);
  });

  it("separates rows with a hairline rather than boxing each one, which is what makes a list a list and not a grid of cards", () => {
    const wrapper = mount(RecordsList, {
      props: { rows, titleTemplate: "${name}" },
    });
    const items = wrapper.findAll("li");
    expect(items.length).toBe(rows.length);
    expect(items[0].classes()).toContain("border-t");
    expect(items[0].classes()).toContain("first:border-t-0");
    expect(items[0].classes()).not.toContain("border");
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(RecordsList, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const firstAnchor = wrapper.find("li a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("hover:underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(RecordsList, {
      props: { rows, titleTemplate: "${name}" },
    });

    expect(wrapper.find("li a").exists()).toBe(false);
  });

  it("renders the logo image with the record's title as alt when logoColumn resolves to a file value", () => {
    const logoColumn: IColumn = {
      id: "logo",
      label: "Logo",
      columnType: "FILE",
    };
    const wrapper = mount(RecordsList, {
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
    expect(img.attributes("alt")).toBe("Tweety");
  });

  it("renders no logo when logoColumn does not resolve to a file value", () => {
    const logoColumn: IColumn = {
      id: "logo",
      label: "Logo",
      columnType: "STRING",
    };
    const wrapper = mount(RecordsList, {
      props: {
        rows: [{ name: "Tweety", age: 3, logo: "not-a-file" }],
        titleTemplate: "${name}",
        logoColumn,
      },
    });

    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(RecordsList, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        titleTemplate: "",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const item = wrapper.find("li");
    const anchor = item.find("a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(item.text()).not.toContain("LL");
    expect(item.text()).not.toContain("2006");
  });

  it("lets Pairs' own hideEmpty default apply when List is not given one, and forwards an explicit false", () => {
    // vue-test-utils' props() reports the CHILD's already-resolved value,
    // defaults included, so it cannot distinguish "forwarded true" from
    // "forwarded undefined, defaulted to true" on its own; the rendered
    // data-fold-columns attribute (Pairs.test.ts covers what it means) is
    // what actually reveals whether the default reached Pairs. A
    // boolean-typed prop that is never passed resolves to Vue's automatic
    // false rather than undefined unless the component declares an
    // explicit default, so List must give hideEmpty an explicit
    // `undefined` default of its own to forward a genuine undefined.
    const bColumn: IColumn = { id: "b", label: "B", columnType: "INT" };
    const omittedWrapper = mount(RecordsList, {
      props: {
        rows,
        titleTemplate: "${name}",
        detailColumns: [ageColumn, bColumn],
      },
    });
    expect(omittedWrapper.find("dl").attributes("data-fold-columns")).toBe("2");

    const disabledWrapper = mount(RecordsList, {
      props: {
        rows,
        titleTemplate: "${name}",
        detailColumns: [ageColumn, bColumn],
        hideEmpty: false,
      },
    });
    expect(
      disabledWrapper.find("dl").attributes("data-fold-columns")
    ).toBeUndefined();
  });

  it("renders the description through value/EMX2.vue", () => {
    const wrapper = mount(RecordsList, {
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
    const wrapper = mount(RecordsList, {
      props: { rows, titleTemplate: "${name}" },
    });

    expect(wrapper.find("li span.mt-1\\.5").exists()).toBe(false);
  });

  it("renders the subtitle beside the title when subtitleTemplate is set", () => {
    const wrapper = mount(RecordsList, {
      props: {
        rows,
        titleTemplate: "${age}",
        subtitleTemplate: "${name}",
      },
    });

    const item = wrapper.findAll("li")[0];
    expect(item.find("h2").text()).toBe("3");
    const subtitle = item.find("span.mt-1\\.5");
    expect(subtitle.exists()).toBe(true);
    expect(subtitle.text()).toBe("Tweety");
  });

  it("promotes the subtitle into the title slot, and renders no subtitle element, when the title is empty", () => {
    const wrapper = mount(RecordsList, {
      props: {
        rows,
        titleTemplate: "",
        subtitleTemplate: "${name}",
      },
    });

    const item = wrapper.findAll("li")[0];
    expect(item.find("h2").text()).toBe("Tweety");
    expect(item.find("span.mt-1\\.5").exists()).toBe(false);
  });
});
