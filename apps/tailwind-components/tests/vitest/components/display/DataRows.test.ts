import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataRows from "../../../../app/components/display/DataRows.vue";
import DataPairs from "../../../../app/components/display/DataPairs.vue";
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
  layout: "LIST",
  titleTemplate: "${name}",
  descriptionTemplate: "A bird named ${name}",
  detailColumns: [ageColumn],
};

describe("DataRows.vue", () => {
  it("renders one row per record, with title and description, and passes its detail columns to DataPairs in the wide shape", () => {
    const wrapper = mount(DataRows, {
      props: { rows, resolved },
    });

    const items = wrapper.findAll("li");
    expect(items.length).toBe(2);
    expect(items[0].text()).toContain("Tweety");
    expect(items[0].text()).toContain("A bird named Tweety");

    const pairsComponents = wrapper.findAllComponents(DataPairs);
    expect(pairsComponents.length).toBe(2);
    expect(pairsComponents[0].props("columns")).toEqual(resolved.detailColumns);
    expect(pairsComponents[0].props("row")).toEqual(rows[0]);
    expect(pairsComponents[0].props("wide")).toBe(true);
  });

  it("lays rows out in a single column", () => {
    const wrapper = mount(DataRows, { props: { rows, resolved } });
    expect(wrapper.find("ul").classes()).toContain("grid-cols-1");
    expect(wrapper.find("ul").classes()).not.toContain("lg:grid-cols-2");
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(DataRows, {
      props: { rows, resolved, linkTo: (row: IRow) => `/records/${row.name}` },
    });

    const firstAnchor = wrapper.find("li a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataRows, {
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
    const wrapper = mount(DataRows, {
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
    const wrapper = mount(DataRows, {
      props: {
        rows: [{ name: "Tweety", age: 3, logo: "not-a-file" }],
        resolved: resolvedWithLogo,
      },
    });

    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("renders no title text when resolved.titleTemplate is empty", () => {
    const emptyResolved: ResolvedDisplay = {
      layout: "LIST",
      titleTemplate: "",
      detailColumns: [],
    };
    const wrapper = mount(DataRows, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        resolved: emptyResolved,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const item = wrapper.find("li");
    const anchor = item.find("a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(item.text()).not.toContain("LL");
    expect(item.text()).not.toContain("2006");
  });

  it("borders every row with plain `border`, never `border-theme`, whose --border-width-theme is 0 in four themes", () => {
    const wrapper = mount(DataRows, { props: { rows, resolved } });
    const items = wrapper.findAll("li");
    expect(items[0].classes()).toContain("border");
    expect(items[0].classes()).not.toContain("border-theme");
  });

  it("forwards maxLines, renderLimit, truncate and hideListSeparator to DataPairs unchanged", () => {
    // The forwarding from DataPairs down to value/EMX2.vue, and the fold,
    // cap and hideEmpty behaviour, are DataPairs' own responsibility,
    // covered in DataPairs.test.ts; this only checks the hop DataRows
    // itself owns.
    const wrapper = mount(DataRows, {
      props: {
        rows,
        resolved,
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

  it("lets DataPairs' own hideEmpty default apply when DataRows is not given one, and forwards an explicit false", () => {
    // vue-test-utils' props() reports the CHILD's already-resolved value,
    // defaults included, so it cannot distinguish "forwarded true" from
    // "forwarded undefined, defaulted to true" on its own; the rendered
    // data-fold-columns attribute (DataPairs.test.ts covers what it means)
    // is what actually reveals whether the default reached DataPairs. A
    // boolean-typed prop that is never passed resolves to Vue's automatic
    // false rather than undefined unless the component declares an
    // explicit default, so DataRows must give hideEmpty an explicit
    // `undefined` default of its own to forward a genuine undefined.
    const twoColumnsResolved: ResolvedDisplay = {
      ...resolved,
      detailColumns: [ageColumn, { id: "b", label: "B", columnType: "INT" }],
    };
    const omittedWrapper = mount(DataRows, {
      props: { rows, resolved: twoColumnsResolved },
    });
    expect(omittedWrapper.find("dl").attributes("data-fold-columns")).toBe("2");

    const disabledWrapper = mount(DataRows, {
      props: { rows, resolved: twoColumnsResolved, hideEmpty: false },
    });
    expect(
      disabledWrapper.find("dl").attributes("data-fold-columns")
    ).toBeUndefined();
  });

  it("forwards maxLines and truncate to ShowMore, without a default of its own, so the description is clamped", () => {
    const wrapper = mount(DataRows, {
      props: {
        rows,
        resolved,
        maxLines: 7,
        truncate: false,
      },
    });

    const showMore = wrapper.findComponent(ShowMore);
    expect(showMore.exists()).toBe(true);
    expect(showMore.props("maxLines")).toBe(7);
    expect(showMore.props("truncate")).toBe(false);
  });
});
