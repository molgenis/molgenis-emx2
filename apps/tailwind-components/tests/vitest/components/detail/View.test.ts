import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, test } from "vitest";
import type {
  ColumnType,
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";
import DetailView from "../../../../app/components/detail/View.vue";

function column(id: string, columnType: ColumnType, label?: string): IColumn {
  return { id, label: label ?? id, columnType };
}

function table(columns: IColumn[]): ITableMetaData {
  return {
    id: "Pet",
    schemaId: "pet store",
    name: "Pet",
    label: "Pet",
    tableType: "DATA",
    columns,
  };
}

const twoSections = table([
  column("about", "SECTION", "About"),
  column("name", "STRING", "Name"),
  column("size", "HEADING", "Size"),
  column("weight", "DECIMAL", "Weight"),
  column("care", "SECTION", "Care"),
  column("diet", "STRING", "Diet"),
]);

const twoSectionsRow: IRow = { name: "spike", weight: 15.7, diet: "insects" };

function menuLinks(wrapper: ReturnType<typeof mount>) {
  const nav = wrapper.find("nav");
  return nav.exists()
    ? nav
        .findAll("a")
        .map((link) => [link.text(), link.attributes("href")] as const)
    : [];
}

describe("DetailView", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(() => {
    wrapper = mount(DetailView, {
      props: { metadata: twoSections, rowData: twoSectionsRow },
    });
  });

  test("renders a section and each of its headings as sibling boxes, in reading order", () => {
    expect(
      wrapper.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["about", "size", "care"]);
    expect(wrapper.get("#about").find("#size").exists()).toBe(false);
  });

  test("titles a heading box one level below a section box", () => {
    expect(wrapper.get("#about").get("h2").text()).toBe("About");
    expect(wrapper.get("#size").get("h3").text()).toBe("Size");
    expect(wrapper.get("#size").find("h2").exists()).toBe(false);
  });

  test("renders no box for a top section holding nothing but headings, and aims its menu entry at the first box it does render", () => {
    const headingsOnly = mount(DetailView, {
      props: {
        metadata: table([
          column("mg_top_of_form", "SECTION", "_top"),
          column("details", "HEADING", "Details"),
          column("name", "STRING", "Name"),
          column("care", "SECTION", "Care"),
          column("diet", "STRING", "Diet"),
        ]),
        rowData: { name: "spike", diet: "insects" },
      },
    });

    expect(
      headingsOnly.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["details", "care"]);
    expect(menuLinks(headingsOnly)).toEqual([
      ["Pet", "#details"],
      ["Details", "#details"],
      ["Care", "#care"],
    ]);
    expect(
      menuLinks(headingsOnly).map(
        ([, href]) => href && headingsOnly.find(href).exists()
      )
    ).toEqual([true, true, true]);
  });

  test("renders each field label and value", () => {
    const text = wrapper.get("#about").text();
    expect(text).toContain("Name");
    expect(text).toContain("spike");
    expect(wrapper.get("#care").text()).toContain("insects");
  });

  test("lists one menu entry per rendered section, targeting its anchor", () => {
    expect(menuLinks(wrapper)).toEqual([
      ["About", "#about"],
      ["Size", "#size"],
      ["Care", "#care"],
    ]);
  });

  test("nests a heading entry inside its own section's entry", () => {
    const topLevel = wrapper.get("nav").findAll(":scope > ul > li");

    expect(topLevel.map((entry) => entry.get("a").attributes("href"))).toEqual([
      "#about",
      "#care",
    ]);
    expect(
      topLevel[0]!.findAll("ul a").map((link) => link.attributes("href"))
    ).toEqual(["#size"]);
    expect(topLevel[1]!.findAll("ul a")).toEqual([]);
  });

  test("names the unnamed top section after the table in the menu, and leaves the section itself unheaded", () => {
    const topped = mount(DetailView, {
      props: {
        metadata: table([
          column("mg_top_of_form", "SECTION", "_top"),
          column("name", "STRING", "Name"),
          column("care", "SECTION", "Care"),
          column("diet", "STRING", "Diet"),
        ]),
        rowData: { name: "spike", diet: "insects" },
      },
    });

    expect(menuLinks(topped)).toEqual([
      ["Pet", "#mg_top_of_form"],
      ["Care", "#care"],
    ]);
    expect(topped.get("#mg_top_of_form").find("h2").exists()).toBe(false);
  });

  test("renders no menu below two sections", () => {
    const single = mount(DetailView, {
      props: {
        metadata: table([
          column("about", "SECTION", "About"),
          column("name", "STRING", "Name"),
        ]),
        rowData: { name: "spike" },
      },
    });

    expect(single.find("nav").exists()).toBe(false);
    expect(single.get("#about").text()).toContain("spike");
  });

  test("drops a filtered-out section from the sections and from the menu", async () => {
    await wrapper.get('input[type="search"]').setValue("diet");

    expect(
      wrapper.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["care"]);
    expect(menuLinks(wrapper)).toEqual([]);
  });

  test("keeps the filter box labelled for a screen reader", () => {
    const input = wrapper.get('input[type="search"]');
    expect(wrapper.get(`label[for="${input.attributes("id")}"]`).text()).toBe(
      "Filter fields"
    );
  });

  test("renders neither menu nor filter when both are switched off", () => {
    const bare = mount(DetailView, {
      props: {
        metadata: twoSections,
        rowData: twoSectionsRow,
        showMenu: false,
        showFilter: false,
      },
    });

    expect(bare.find("nav").exists()).toBe(false);
    expect(bare.find('input[type="search"]').exists()).toBe(false);
    expect(
      bare.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["about", "size", "care"]);
  });

  test("shows mg_ columns only when asked", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING", "Name"),
      column("mg_insertedBy", "STRING", "Inserted by"),
    ]);
    const rowData: IRow = { name: "spike", mg_insertedBy: "admin" };

    expect(
      mount(DetailView, { props: { metadata, rowData } }).text()
    ).not.toContain("Inserted by");
    expect(
      mount(DetailView, {
        props: { metadata, rowData, showMgColumns: true },
      }).text()
    ).toContain("Inserted by");
  });
});
