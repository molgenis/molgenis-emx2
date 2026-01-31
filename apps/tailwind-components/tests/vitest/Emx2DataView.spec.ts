import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import { ref, nextTick } from "vue";
import Emx2DataView from "../../app/components/display/Emx2DataView.vue";
import type { ITableMetaData } from "../../../metadata-utils/src/types";

const mockMetadata: ITableMetaData = {
  id: "TestTable",
  name: "TestTable",
  schemaId: "TestSchema",
  label: "Test Table",
  tableType: "DATA",
  columns: [
    { id: "id", label: "ID", columnType: "STRING", key: 1 },
    { id: "name", label: "Name", columnType: "STRING" },
    { id: "age", label: "Age", columnType: "INT" },
  ],
};

const mockRows = [
  { id: "1", name: "Row 1", age: 25 },
  { id: "2", name: "Row 2", age: 30 },
];

const mockFetchTableMetadata = vi.fn();
const mockFetchTableData = vi.fn();

vi.mock("#imports", () => ({
  fetchTableMetadata: (...args: any[]) => mockFetchTableMetadata(...args),
  fetchTableData: (...args: any[]) => mockFetchTableData(...args),
}));

vi.mock("../../app/composables/fetchTableMetadata", () => ({
  default: (...args: any[]) => mockFetchTableMetadata(...args),
}));

vi.mock("../../app/composables/fetchTableData", () => ({
  default: (...args: any[]) => mockFetchTableData(...args),
}));

const mockRoute = { query: {} };
const mockRouter = { replace: vi.fn(), push: vi.fn() };

vi.mock("vue-router", () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

describe("Emx2DataView", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchTableMetadata.mockResolvedValue(mockMetadata);
    mockFetchTableData.mockResolvedValue({ rows: mockRows, count: 2 });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("DetailPageLayout Integration", () => {
    it("uses DetailPageLayout internally", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template:
                '<div class="detail-page-layout"><slot name="header" /><slot name="sidebar" /><slot name="main" /></div>',
            },
          },
        },
      });

      await nextTick();
      expect(wrapper.find(".detail-page-layout").exists()).toBe(true);
    });

    it("passes #header slot through when provided", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
        },
        slots: {
          header: '<div class="test-header">Custom Header</div>',
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template: '<div><slot name="header" /><slot name="main" /></div>',
            },
          },
        },
      });

      await nextTick();
      expect(wrapper.find(".test-header").exists()).toBe(true);
      expect(wrapper.text()).toContain("Custom Header");
    });

    it("#sidebar slot contains FilterSidebar when showFilters=true", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: true },
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template:
                '<div><slot name="sidebar" class="sidebar-slot" /><slot name="main" /></div>',
            },
            FilterSidebar: {
              template: '<div class="filter-sidebar">Filters</div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      expect(wrapper.find(".filter-sidebar").exists()).toBe(true);
    });

    it("no sidebar when showFilters=false", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: false },
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template:
                '<div><slot name="sidebar" /><slot name="main" /></div>',
              props: ["showSideNav"],
            },
            FilterSidebar: {
              template: '<div class="filter-sidebar">Filters</div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      expect(wrapper.find(".filter-sidebar").exists()).toBe(false);
    });
  });

  describe("View Modes", () => {
    it("Full Page: showFilters=true, header slot provided", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: true },
        },
        slots: {
          header: '<div class="header">Header</div>',
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template:
                '<div><slot name="header" /><slot name="sidebar" /><slot name="main" /></div>',
              props: ["showSideNav"],
            },
            FilterSidebar: {
              template: '<div class="filter-sidebar">Filters</div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      expect(wrapper.find(".header").exists()).toBe(true);
      expect(wrapper.find(".filter-sidebar").exists()).toBe(true);
    });

    it("Compact: showFilters=true, no header slot", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: true },
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template:
                '<div><slot name="header" /><slot name="sidebar" /><slot name="main" /></div>',
              props: ["showSideNav"],
            },
            FilterSidebar: {
              template: '<div class="filter-sidebar">Filters</div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      expect(wrapper.find(".header").exists()).toBe(false);
      expect(wrapper.find(".filter-sidebar").exists()).toBe(true);
    });

    it("Vanilla: showFilters=false", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: false },
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template:
                '<div><slot name="sidebar" /><slot name="main" /></div>',
              props: ["showSideNav"],
            },
            FilterSidebar: {
              template: '<div class="filter-sidebar">Filters</div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      expect(wrapper.find(".filter-sidebar").exists()).toBe(false);
    });
  });

  describe("Responsive Filters", () => {
    it("mobile filter button exists when showFilters=true (class xl:hidden)", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: true },
        },
        global: {
          stubs: {
            SideModal: {
              template: '<div class="side-modal"><slot name="button" /></div>',
            },
            Button: {
              template: '<button class="mobile-filter-btn">Filters</button>',
              props: ["label", "icon", "type", "size", "iconPosition"],
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      const mobileFilterContainer = wrapper.find(".xl\\:hidden");
      expect(mobileFilterContainer.exists()).toBe(true);
      expect(mobileFilterContainer.find(".side-modal").exists()).toBe(true);
    });

    it("mobile filter button NOT rendered when showFilters=false", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { showFilters: false },
        },
        global: {
          stubs: {
            FilterSidebar: {
              template: '<div class="filter-sidebar-stub">Filters</div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      const mobileContainer = wrapper.find(".xl\\:hidden");
      expect(mobileContainer.exists()).toBe(true);
      expect(mobileContainer.find(".filter-sidebar-stub").exists()).toBe(false);
    });
  });

  describe("Layout Modes", () => {
    it("layout=table: renders table element", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { layout: "table" },
        },
      });

      await nextTick();
      await nextTick();
      await nextTick();
      expect(wrapper.find("table").exists()).toBe(true);
      expect(wrapper.find("ul").exists()).toBe(false);
    });

    it("layout=list: renders ul element", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { layout: "list" },
        },
      });

      await nextTick();
      await nextTick();
      await nextTick();
      const ul = wrapper.find("ul");
      expect(ul.exists()).toBe(true);
      expect(ul.classes()).toContain("grid");
      expect(wrapper.find("table").exists()).toBe(false);
    });

    it("layout=cards: renders CardList", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { layout: "cards" },
        },
        global: {
          stubs: {
            CardList: {
              template: '<div class="card-list"><slot /></div>',
            },
            CardListItem: {
              template: '<div class="card-item"><slot /></div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      await nextTick();
      expect(wrapper.find(".card-list").exists()).toBe(true);
      expect(wrapper.find("table").exists()).toBe(false);
      expect(wrapper.find("ul").exists()).toBe(false);
    });
  });

  describe("Slots", () => {
    it("#header slot renders content", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
        },
        slots: {
          header: '<div class="custom-header">Test Header</div>',
        },
        global: {
          stubs: {
            DetailPageLayout: {
              template: '<div><slot name="header" /><slot name="main" /></div>',
            },
          },
        },
      });

      await nextTick();
      expect(wrapper.find(".custom-header").exists()).toBe(true);
      expect(wrapper.text()).toContain("Test Header");
    });

    it("#default slot for list items", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { layout: "list" },
        },
        slots: {
          default: `<template #default="{ row, label }">
            <span class="custom-list-item">{{ label }} - {{ row.age }}</span>
          </template>`,
        },
      });

      await nextTick();
      await nextTick();
      await nextTick();
      expect(wrapper.find(".custom-list-item").exists()).toBe(true);
    });

    it("#card slot for card content", async () => {
      const wrapper = mount(Emx2DataView, {
        props: {
          schemaId: "TestSchema",
          tableId: "TestTable",
          config: { layout: "cards" },
        },
        slots: {
          card: `<template #card="{ row, label }">
            <div class="custom-card">{{ label }}</div>
          </template>`,
        },
        global: {
          stubs: {
            CardList: {
              template: '<div class="card-list"><slot /></div>',
            },
            CardListItem: {
              template: '<div class="card-item"><slot /></div>',
            },
          },
        },
      });

      await nextTick();
      await nextTick();
      await nextTick();
      expect(wrapper.find(".custom-card").exists()).toBe(true);
    });
  });
});
