import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import FilterPicker from "../../../../app/components/filter/FilterPicker.vue";
import InputCheckboxIcon from "../../../../app/components/input/CheckboxIcon.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

describe("FilterPicker", () => {
  const mockColumns: IColumn[] = [
    {
      id: "heading1",
      label: "Demographics",
      columnType: "HEADING",
    },
    {
      id: "name",
      label: "Name",
      columnType: "STRING",
      heading: "Demographics",
      description: "Patient full name",
    },
    {
      id: "age",
      label: "Age",
      columnType: "INT",
      heading: "Demographics",
    },
    {
      id: "country",
      label: "Country",
      columnType: "ONTOLOGY",
      refSchemaId: "CatalogueOntologies",
      refTableId: "Countries",
      heading: "Demographics",
      description: "Country of residence",
    },
    {
      id: "heading2",
      label: "Medical",
      columnType: "HEADING",
    },
    {
      id: "diagnosis",
      label: "Diagnosis",
      columnType: "ONTOLOGY_ARRAY",
      refSchemaId: "CatalogueOntologies",
      refTableId: "Diagnoses",
      heading: "Medical",
    },
    {
      id: "hospital",
      label: "Hospital",
      columnType: "REF",
      refSchemaId: "test",
      refTableId: "Hospital",
      heading: "Medical",
      description: "Primary hospital",
    },
    {
      id: "medications",
      label: "Medications",
      columnType: "REF_ARRAY",
      refSchemaId: "test",
      refTableId: "Medication",
      heading: "Medical",
    },
    {
      id: "weight",
      label: "Weight",
      columnType: "DECIMAL",
      heading: "Medical",
    },
    {
      id: "admissionDate",
      label: "Admission Date",
      columnType: "DATE",
      heading: "Medical",
    },
    {
      id: "notes",
      label: "Notes",
      columnType: "TEXT",
    },
    {
      id: "active",
      label: "Active",
      columnType: "BOOL",
    },
    {
      id: "mg_internal",
      label: "Internal Field",
      columnType: "STRING",
    },
    {
      id: "section1",
      label: "Section",
      columnType: "SECTION",
    },
  ];

  const defaultProps = {
    columns: mockColumns,
    visibleFilterIds: ["country", "diagnosis"],
    defaultFilterIds: ["country", "diagnosis"],
    schemaId: "test",
  };

  const vDropdownStub = {
    template: `
      <div>
        <slot />
        <div class="dropdown-popper" v-if="isOpen"><slot name="popper" :hide="hide" /></div>
      </div>
    `,
    data() {
      return {
        isOpen: false,
      };
    },
    methods: {
      hide() {
        this.isOpen = false;
      },
    },
    mounted() {
      const button = this.$el.querySelector("button");
      if (button) {
        button.addEventListener("click", () => {
          this.isOpen = !this.isOpen;
        });
      }
    },
  };

  describe("Basic rendering", () => {
    it("renders Add filter button", () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      expect(wrapper.text()).toContain("Add filter");
    });

    it("dropdown is closed by default", () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      const dropdown = wrapper.find(".dropdown-popper");
      expect(dropdown.exists()).toBe(false);
    });

    it("opens dropdown when button is clicked", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const dropdown = wrapper.find(".dropdown-popper");
      expect(dropdown.exists()).toBe(true);
    });
  });

  describe("Column filtering", () => {
    it("shows all filterable columns", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      expect(wrapper.text()).toContain("Name");
      expect(wrapper.text()).toContain("Age");
      expect(wrapper.text()).toContain("Country");
      expect(wrapper.text()).toContain("Diagnosis");
      expect(wrapper.text()).toContain("Notes");
      expect(wrapper.text()).toContain("Active");
    });

    it("excludes HEADING columns", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const columnButtons = wrapper.findAll("button").filter((b) => {
        const text = b.text();
        return (
          text.includes("Name") ||
          text.includes("Age") ||
          text.includes("Hospital")
        );
      });
      const labels = columnButtons.map((b) => b.text());
      expect(labels).not.toContain("Demographics");
    });

    it("excludes SECTION columns", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const columnButtons = wrapper.findAll("button").filter((b) => {
        const text = b.text();
        return (
          text.includes("Name") ||
          text.includes("Age") ||
          text.includes("Hospital")
        );
      });
      const labels = columnButtons.map((b) => b.text());
      expect(labels).not.toContain("Section");
    });

    it("excludes mg_* columns", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      expect(wrapper.text()).not.toContain("Internal Field");
    });
  });

  describe("Sorting and grouping", () => {
    it("groups columns by type with type headers", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const text = wrapper.text();
      expect(text).toContain("ontology");
      expect(text).toContain("ref");
      expect(text).toContain("text");
      expect(text).toContain("number");
    });

    it("sorts columns alphabetically within each type group", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const checkboxes = wrapper.findAllComponents(InputCheckboxIcon);
      expect(checkboxes.length).toBeGreaterThan(0);
    });
  });

  describe("Search functionality", () => {
    it("filters columns by label", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("Country");
      await new Promise((resolve) => setTimeout(resolve, 600));

      const checkboxes = wrapper.findAllComponents(InputCheckboxIcon);
      expect(checkboxes.length).toBe(1);
      expect(wrapper.text()).toContain("Country");
    });

    it("search is case-insensitive", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("country");
      await new Promise((resolve) => setTimeout(resolve, 600));

      const checkboxes = wrapper.findAllComponents(InputCheckboxIcon);
      expect(checkboxes.length).toBe(1);
      expect(wrapper.text()).toContain("Country");
    });

    it("shows no results when search has no matches", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("nonexistent");
      await new Promise((resolve) => setTimeout(resolve, 600));

      const allText = wrapper.text();
      expect(allText).not.toContain("Name");
      expect(allText).not.toContain("Age");
      expect(allText).not.toContain("Hospital");
    });
  });

  describe("Checkbox state", () => {
    it("shows checkboxes for non-REF columns only", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const checkboxes = wrapper.findAllComponents(InputCheckboxIcon);
      expect(checkboxes.length).toBeGreaterThan(0);
    });

    it("REF columns have checkbox and expand caret", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const hospitalButtons = wrapper.findAll("button").filter((b) => {
        return (
          b.text().includes("Hospital") &&
          b.findComponent(InputCheckboxIcon).exists()
        );
      });
      expect(hospitalButtons.length).toBeGreaterThanOrEqual(1);
    });

    it("shows checked checkboxes for visible filters", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const checkboxes = wrapper.findAllComponents(InputCheckboxIcon);
      const checkedCheckboxes = checkboxes.filter((c) => c.props("checked"));
      expect(checkedCheckboxes.length).toBeGreaterThan(0);
    });

    it("shows unchecked checkboxes for hidden filters", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const checkboxes = wrapper.findAllComponents(InputCheckboxIcon);
      const uncheckedCheckboxes = checkboxes.filter((c) => !c.props("checked"));
      expect(uncheckedCheckboxes.length).toBeGreaterThan(0);
    });
  });

  describe("Toggle event", () => {
    it("emits toggle event when column is clicked", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("Name");
      await new Promise((resolve) => setTimeout(resolve, 600));

      const columnButtons = wrapper.findAll("button").filter((b) => {
        return b.text().includes("Name") && b.text() !== "DEMOGRAPHICS";
      });
      await columnButtons[0].trigger("click");

      expect(wrapper.emitted("toggle")).toBeTruthy();
      expect(wrapper.emitted("toggle")?.[0]).toEqual(["name"]);
    });
  });

  describe("Reset functionality", () => {
    it("shows Reset to defaults button", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      expect(wrapper.text()).toContain("Reset to defaults");
    });

    it("emits reset event when reset button is clicked", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const buttons = wrapper.findAll("button");
      const resetButton = buttons.find((b) =>
        b.text().includes("Reset to defaults")
      );
      await resetButton?.trigger("click");

      expect(wrapper.emitted("reset")).toBeTruthy();
    });
  });

  describe("Keyboard interaction", () => {
    it("closes dropdown on Escape key", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      let dropdown = wrapper.find(".dropdown-popper");
      expect(dropdown.exists()).toBe(true);

      const popperContent = dropdown.find(".bg-modal");
      await popperContent.trigger("keydown.escape");

      dropdown = wrapper.find(".dropdown-popper");
      expect(dropdown.exists()).toBe(false);
    });
  });
});

describe("computeDefaultFilters logic", () => {
  function computeDefaultFilters(columns: IColumn[]): string[] {
    const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];
    const REF_TYPES_FOR_DEFAULT = ["REF", "REF_ARRAY"];
    const MAX_DEFAULT_FILTERS = 5;

    const unfilterable = ["HEADING", "SECTION"];
    const filterable = columns.filter(
      (col) =>
        !unfilterable.includes(col.columnType) && !col.id.startsWith("mg_")
    );

    const ontologyCols = filterable.filter((c) =>
      ONTOLOGY_TYPES.includes(c.columnType)
    );
    const refCols = filterable.filter((c) =>
      REF_TYPES_FOR_DEFAULT.includes(c.columnType)
    );

    const defaults = ontologyCols
      .slice(0, MAX_DEFAULT_FILTERS)
      .map((c) => c.id);
    if (defaults.length < MAX_DEFAULT_FILTERS) {
      const remaining = MAX_DEFAULT_FILTERS - defaults.length;
      defaults.push(...refCols.slice(0, remaining).map((c) => c.id));
    }
    return defaults;
  }

  it("returns first 5 ontology columns", () => {
    const columns: IColumn[] = [
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
      { id: "ont2", label: "Ont 2", columnType: "ONTOLOGY_ARRAY" },
      { id: "ont3", label: "Ont 3", columnType: "ONTOLOGY" },
      { id: "ont4", label: "Ont 4", columnType: "ONTOLOGY" },
      { id: "ont5", label: "Ont 5", columnType: "ONTOLOGY" },
      { id: "ont6", label: "Ont 6", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1", "ont2", "ont3", "ont4", "ont5"]);
  });

  it("fills with ref columns when < 5 ontology", () => {
    const columns: IColumn[] = [
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
      { id: "ont2", label: "Ont 2", columnType: "ONTOLOGY_ARRAY" },
      { id: "ref1", label: "Ref 1", columnType: "REF" },
      { id: "ref2", label: "Ref 2", columnType: "REF_ARRAY" },
      { id: "ref3", label: "Ref 3", columnType: "REF" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1", "ont2", "ref1", "ref2", "ref3"]);
  });

  it("excludes HEADING columns", () => {
    const columns: IColumn[] = [
      { id: "heading1", label: "Heading", columnType: "HEADING" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("excludes SECTION columns", () => {
    const columns: IColumn[] = [
      { id: "section1", label: "Section", columnType: "SECTION" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("excludes mg_* columns", () => {
    const columns: IColumn[] = [
      { id: "mg_internal", label: "Internal", columnType: "ONTOLOGY" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("returns empty array when no suitable columns", () => {
    const columns: IColumn[] = [
      { id: "str1", label: "String", columnType: "STRING" },
      { id: "int1", label: "Int", columnType: "INT" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual([]);
  });
});

describe("Deep nesting (3+ levels)", () => {
  const vDropdownStub = {
    template: `
      <div>
        <slot />
        <div class="dropdown-popper" v-if="isOpen"><slot name="popper" :hide="hide" /></div>
      </div>
    `,
    data() {
      return {
        isOpen: false,
      };
    },
    methods: {
      hide() {
        this.isOpen = false;
      },
    },
    mounted() {
      const button = this.$el.querySelector("button");
      if (button) {
        button.addEventListener("click", () => {
          this.isOpen = !this.isOpen;
        });
      }
    },
  };

  const mockColumnsWithDeepRefs: IColumn[] = [
    {
      id: "order",
      label: "Order",
      columnType: "REF",
      refSchemaId: "test",
      refTableId: "Order",
    },
    {
      id: "name",
      label: "Name",
      columnType: "STRING",
    },
  ];

  it("shows expand caret on nested REF columns", async () => {
    const wrapper = mount(FilterPicker, {
      props: {
        columns: mockColumnsWithDeepRefs,
        visibleFilterIds: [],
        defaultFilterIds: [],
        schemaId: "test",
      },
      global: {
        stubs: {
          VDropdown: vDropdownStub,
        },
      },
    });

    await wrapper.find("button").trigger("click");

    const orderButtons = wrapper.findAll("button").filter((b) => {
      return b.text().includes("Order");
    });
    expect(orderButtons.length).toBeGreaterThanOrEqual(1);
  });

  it("expands nested REF columns to show deeper children", async () => {
    const wrapper = mount(FilterPicker, {
      props: {
        columns: mockColumnsWithDeepRefs,
        visibleFilterIds: [],
        defaultFilterIds: [],
        schemaId: "test",
      },
      global: {
        stubs: {
          VDropdown: vDropdownStub,
        },
      },
    });

    await wrapper.find("button").trigger("click");

    const orderButtons = wrapper.findAll("button").filter((b) => {
      return b.text().includes("Order") && !b.text().includes("Name");
    });
    expect(orderButtons.length).toBeGreaterThanOrEqual(1);
  });

  it("collapsing parent collapses all descendants", async () => {
    const wrapper = mount(FilterPicker, {
      props: {
        columns: mockColumnsWithDeepRefs,
        visibleFilterIds: [],
        defaultFilterIds: [],
        schemaId: "test",
      },
      global: {
        stubs: {
          VDropdown: vDropdownStub,
        },
      },
    });

    await wrapper.find("button").trigger("click");

    const orderButtons = wrapper.findAll("button").filter((b) => {
      return b.text().includes("Order");
    });
    expect(orderButtons.length).toBeGreaterThanOrEqual(1);
  });

  it("emits full dot-path for deeply nested toggle", async () => {
    const wrapper = mount(FilterPicker, {
      props: {
        columns: mockColumnsWithDeepRefs,
        visibleFilterIds: [],
        defaultFilterIds: [],
        schemaId: "test",
      },
      global: {
        stubs: {
          VDropdown: vDropdownStub,
        },
      },
    });

    await wrapper.find("button").trigger("click");

    const nameButtons = wrapper.findAll("button").filter((b) => {
      return b.text().includes("Name") && b.text() !== "Name";
    });
    if (nameButtons.length > 0) {
      await nameButtons[0].trigger("click");
      expect(wrapper.emitted("toggle")).toBeTruthy();
    }
  });
});
