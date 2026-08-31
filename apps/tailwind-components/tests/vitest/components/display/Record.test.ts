import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { nextTick } from "vue";
import type {
  ColumnType,
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";
import DisplayRecord from "../../../../app/components/display/Record.vue";

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

class FakeIntersectionObserver {
  static instances: FakeIntersectionObserver[] = [];
  disconnected = false;
  targets: Element[] = [];
  constructor(
    public callback: (entries: { isIntersecting: boolean }[]) => void,
    public options?: IntersectionObserverInit
  ) {
    FakeIntersectionObserver.instances.push(this);
  }
  observe(target: Element) {
    this.targets.push(target);
  }
  unobserve() {}
  disconnect() {
    this.disconnected = true;
  }
  takeRecords() {
    return [];
  }
}

function reportBox(boxId: string, isIntersecting: boolean) {
  const observer = FakeIntersectionObserver.instances.find((instance) =>
    instance.targets.some((target) => target.id === boxId)
  );
  if (!observer) {
    throw new Error(`no observer watches box ${boxId}`);
  }
  observer.callback([{ isIntersecting }]);
}

function menuCurrent(wrapper: ReturnType<typeof mount>) {
  return wrapper
    .find("nav")
    .findAll("a")
    .map((link) => link.attributes("aria-current"));
}

function menuLinks(wrapper: ReturnType<typeof mount>) {
  const nav = wrapper.find("nav");
  return nav.exists()
    ? nav
        .findAll("a")
        .map((link) => [link.text(), link.attributes("href")] as const)
    : [];
}

describe("DisplayRecord", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(async () => {
    FakeIntersectionObserver.instances = [];
    vi.stubGlobal("IntersectionObserver", FakeIntersectionObserver);
    wrapper = mount(DisplayRecord, {
      props: { metadata: twoSections, rowData: twoSectionsRow },
    });
    // The observers are created on the post-render flush, so they exist only a tick after mount.
    await nextTick();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("renders a section and each of its headings as sibling boxes, in reading order", () => {
    expect(
      wrapper.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["about", "size", "care"]);
    expect(wrapper.get("#about").find("#size").exists()).toBe(false);
  });

  test("titles a heading box one level below a section box", () => {
    expect(wrapper.get("#about").get("h2").text()).toBe("About");
    expect(wrapper.get("#about").find("h3").exists()).toBe(false);
    expect(wrapper.get("#size").get("h3").text()).toBe("Size");
    expect(wrapper.get("#size").find("h2").exists()).toBe(false);
  });

  test("renders no box for a top section holding nothing but headings, and aims its menu entry at the first box it does render", () => {
    const headingsOnly = mount(DisplayRecord, {
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

  test("marks the entry of the box now at the top, only that entry, and keeps it when another leaves the band", async () => {
    reportBox("size", true);
    await wrapper.vm.$nextTick();
    expect(menuCurrent(wrapper)).toEqual(["false", "true", "false"]);

    reportBox("care", true);
    await wrapper.vm.$nextTick();
    expect(menuCurrent(wrapper)).toEqual(["false", "false", "true"]);

    reportBox("size", false);
    await wrapper.vm.$nextTick();
    expect(menuCurrent(wrapper)).toEqual(["false", "false", "true"]);
  });

  test("stops watching its boxes when the record leaves the page", () => {
    expect(
      FakeIntersectionObserver.instances.map((o) => o.disconnected)
    ).toEqual([false, false, false]);

    wrapper.unmount();

    expect(
      FakeIntersectionObserver.instances.map((o) => o.disconnected)
    ).toEqual([true, true, true]);
  });

  test("watches a band across the top of the viewport, which a box taller than it can still cross", () => {
    expect(
      FakeIntersectionObserver.instances.map((instance) =>
        instance.targets.map((target) => target.id)
      )
    ).toEqual([["about"], ["size"], ["care"]]);

    expect(
      FakeIntersectionObserver.instances.map((instance) => instance.options)
    ).toEqual([
      { root: null, rootMargin: "0px 0px -80% 0px", threshold: 0 },
      { root: null, rootMargin: "0px 0px -80% 0px", threshold: 0 },
      { root: null, rootMargin: "0px 0px -80% 0px", threshold: 0 },
    ]);
  });

  test("watches nothing at all when the menu is off", async () => {
    FakeIntersectionObserver.instances = [];

    const noMenu = mount(DisplayRecord, {
      props: {
        metadata: twoSections,
        rowData: twoSectionsRow,
        showMenu: false,
      },
    });
    await nextTick();

    expect(FakeIntersectionObserver.instances).toEqual([]);
    expect(noMenu.find("nav").exists()).toBe(false);
  });

  test("starts watching a box whose tracking turns on after mount", async () => {
    FakeIntersectionObserver.instances = [];

    const latecomer = mount(DisplayRecord, {
      props: {
        metadata: twoSections,
        rowData: twoSectionsRow,
        showMenu: false,
      },
    });
    await nextTick();
    expect(FakeIntersectionObserver.instances).toEqual([]);

    await latecomer.setProps({ showMenu: true });
    await nextTick();

    expect(
      FakeIntersectionObserver.instances.flatMap((instance) =>
        instance.targets.map((target) => target.id)
      )
    ).toContain("about");
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
    const topped = mount(DisplayRecord, {
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

  test("lists the boxes of a lone section as siblings, because nesting them all under it says nothing", () => {
    const oneSection = mount(DisplayRecord, {
      props: {
        metadata: table([
          column("mg_top_of_form", "SECTION", "_top"),
          column("name", "STRING", "Name"),
          column("details", "HEADING", "Details"),
          column("status", "STRING", "Status"),
          column("heading2", "HEADING", "Heading2"),
          column("weight", "DECIMAL", "Weight"),
        ]),
        rowData: { name: "spike", status: "available", weight: 15.7 },
      },
    });

    expect(menuLinks(oneSection)).toEqual([
      ["Pet", "#mg_top_of_form"],
      ["Details", "#details"],
      ["Heading2", "#heading2"],
    ]);
    const topLevel = oneSection.get("nav").findAll(":scope > ul > li");
    expect(topLevel.map((entry) => entry.get("a").attributes("href"))).toEqual([
      "#mg_top_of_form",
      "#details",
      "#heading2",
    ]);
    expect(oneSection.get("nav").findAll("ul ul")).toEqual([]);
  });

  test("titles the menu with every key value of the record, joined", () => {
    const withKeys = (rowData: IRow) =>
      mount(DisplayRecord, {
        props: {
          metadata: table([
            { ...column("name", "STRING", "Name"), key: 1 },
            {
              ...column("category", "REF", "Category"),
              key: 1,
              refSchemaId: "pet store",
              refTableId: "Category",
              refLabel: "${name}",
              refLabelDefault: "${name}",
            },
            column("status", "STRING", "Status"),
            column("details", "HEADING", "Details"),
            column("weight", "DECIMAL", "Weight"),
          ]),
          rowData,
        },
      });

    expect(
      withKeys({
        name: "spike",
        category: { name: "dog" },
        status: "available",
        weight: 15.7,
      })
        .get("nav")
        .get("h2")
        .text()
    ).toBe("spike - dog");
    expect(
      withKeys({ name: "spike", status: "available", weight: 15.7 })
        .get("nav")
        .get("h2")
        .text()
    ).toBe("spike");
  });

  test("reveals the menu at the same width the layout gives it a column, so it never stacks", () => {
    const revealAt = wrapper
      .get("nav")
      .classes()
      .find((name) => name.endsWith(":block"));
    const columnsAt = wrapper
      .get("aside")
      .classes()
      .find((name) => name.endsWith(":sticky"));

    expect(wrapper.get("nav").classes()).toContain("hidden");
    expect(revealAt?.split(":")[0]).toBe(columnsAt?.split(":")[0]);
    expect(revealAt).toBe("lg:block");
    expect(columnsAt).toBe("lg:sticky");
  });

  test("renders no menu below two boxes", () => {
    const single = mount(DisplayRecord, {
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

  test("renders no field filter, beside the menu or above the record", () => {
    expect(wrapper.findAll('input[type="search"]')).toHaveLength(0);
    expect(wrapper.find("header").exists()).toBe(false);

    const bare = mount(DisplayRecord, {
      props: {
        metadata: twoSections,
        rowData: twoSectionsRow,
        showMenu: false,
      },
    });

    expect(bare.find("nav").exists()).toBe(false);
    expect(bare.findAll('input[type="search"]')).toHaveLength(0);
    expect(
      bare.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["about", "size", "care"]);
  });

  test("renders each box as a plain heading and list, with no card and no lg:gap-2.5, when showCards is off", () => {
    const noCards = mount(DisplayRecord, {
      props: {
        metadata: twoSections,
        rowData: twoSectionsRow,
        showCards: false,
      },
    });

    expect(noCards.findAll("section")).toHaveLength(0);
    expect(noCards.get("#about").get("h2").text()).toBe("About");
    expect(noCards.get("#about").text()).toContain("spike");
    expect(noCards.get("main").get("div.grid").classes()).not.toContain(
      "lg:gap-2.5"
    );
  });

  test("puts the field filter at the top of the record column, never the sidebar", () => {
    const withFilter = mount(DisplayRecord, {
      props: {
        metadata: twoSections,
        rowData: twoSectionsRow,
        showFilter: true,
      },
    });

    const main = withFilter.get("main").element;
    const firstChild = main.firstElementChild as HTMLElement;
    expect(firstChild.querySelector('input[type="search"]')).not.toBeNull();
    expect(withFilter.find('aside input[type="search"]').exists()).toBe(false);
  });

  test("filters sections by field label, dropping a box and its menu entry together", async () => {
    const threeFlatSections = table([
      column("about", "SECTION", "About"),
      column("name", "STRING", "Name"),
      column("size", "SECTION", "Size"),
      column("weight", "DECIMAL", "Weight"),
      column("care", "SECTION", "Care"),
      column("diet", "STRING", "Diet"),
    ]);
    const withFilter = mount(DisplayRecord, {
      props: {
        metadata: threeFlatSections,
        rowData: { name: "spike", weight: 15.7, diet: "insects" },
        showFilter: true,
      },
    });

    await withFilter.get('input[type="search"]').setValue("i");

    expect(
      withFilter.findAll("section").map((section) => section.attributes("id"))
    ).toEqual(["size", "care"]);
    expect(menuLinks(withFilter)).toEqual([
      ["Size", "#size"],
      ["Care", "#care"],
    ]);
  });

  function accordionToggle(wrapper: ReturnType<typeof mount>, label: string) {
    const match = wrapper
      .findAll("button")
      .find((button) => button.text() === label);
    if (!match) {
      throw new Error(`no accordion toggle button labelled ${label}`);
    }
    return match;
  }

  test("collapsed wraps the record in an accordion closed by default, headed by the record's key, with no menu and no filter", () => {
    const collapsibleMetadata = table([
      { ...column("name", "STRING", "Name"), key: 1 },
      column("about", "SECTION", "About"),
      column("diet", "STRING", "Diet"),
    ]);
    const collapsed = mount(DisplayRecord, {
      props: {
        metadata: collapsibleMetadata,
        rowData: { name: "spike", diet: "insects" },
        collapsed: true,
        showMenu: true,
        showFilter: true,
      },
    });

    expect(
      accordionToggle(collapsed, "spike").attributes("aria-expanded")
    ).toBe("false");
    expect(collapsed.find("nav").exists()).toBe(false);
    expect(collapsed.find('input[type="search"]').exists()).toBe(false);
  });

  test("expanding the collapsed accordion reveals the whole record, the menu and the filter", async () => {
    const collapsibleMetadata = table([
      { ...column("name", "STRING", "Name"), key: 1 },
      column("about", "SECTION", "About"),
      column("diet", "STRING", "Diet"),
    ]);
    const collapsed = mount(DisplayRecord, {
      props: {
        metadata: collapsibleMetadata,
        rowData: { name: "spike", diet: "insects" },
        collapsed: true,
        showMenu: true,
        showFilter: true,
      },
    });

    await accordionToggle(collapsed, "spike").trigger("click");

    expect(
      accordionToggle(collapsed, "spike").attributes("aria-expanded")
    ).toBe("true");
    expect(collapsed.text()).toContain("insects");
    expect(collapsed.find("nav").exists()).toBe(true);
    expect(collapsed.find('input[type="search"]').exists()).toBe(true);
  });

  test("shows mg_ columns only when asked", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING", "Name"),
      column("mg_insertedBy", "STRING", "Inserted by"),
    ]);
    const rowData: IRow = { name: "spike", mg_insertedBy: "admin" };

    expect(
      mount(DisplayRecord, { props: { metadata, rowData } }).text()
    ).not.toContain("Inserted by");
    expect(
      mount(DisplayRecord, {
        props: { metadata, rowData, showMgColumns: true },
      }).text()
    ).toContain("Inserted by");
  });
});
