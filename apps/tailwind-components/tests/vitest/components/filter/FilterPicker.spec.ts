import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import FilterPicker from "../../../../app/components/filter/FilterPicker.vue";
import InputCheckboxIcon from "../../../../app/components/input/CheckboxIcon.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

describe("FilterPicker", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

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
      <div @click="isOpen = !isOpen">
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

    it("excludes HEADING, SECTION, mg_*, and FILE columns", async () => {
      const columnsWithFile: IColumn[] = [
        ...mockColumns,
        {
          id: "attachment",
          label: "Attachment",
          columnType: "FILE",
        },
      ];

      const wrapper = mount(FilterPicker, {
        props: { ...defaultProps, columns: columnsWithFile },
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      expect(wrapper.text()).not.toContain("Demographics");
      expect(wrapper.text()).not.toContain("Section");
      expect(wrapper.text()).not.toContain("Internal Field");
      expect(wrapper.text()).not.toContain("Attachment");
    });
  });

  describe("Sorting and grouping", () => {
    it("sorts columns alphabetically", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: {
          stubs: {
            VDropdown: vDropdownStub,
          },
        },
      });

      await wrapper.find("button").trigger("click");

      const columnButtons = wrapper
        .find(".dropdown-popper")
        .findAll("button")
        .filter((b) => b.findComponent(InputCheckboxIcon).exists());
      const labels = columnButtons.map((b) => b.find("span").text().trim());
      expect(labels).toEqual([
        "Active",
        "Admission Date",
        "Age",
        "Country",
        "Diagnosis",
        "Hospital",
        "Medications",
        "Name",
        "Notes",
        "Weight",
      ]);
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
      vi.advanceTimersByTime(600);
      await flushPromises();

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
      vi.advanceTimersByTime(600);
      await flushPromises();

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
      vi.advanceTimersByTime(600);
      await flushPromises();

      const allText = wrapper.text();
      expect(allText).not.toContain("Name");
      expect(allText).not.toContain("Age");
      expect(allText).not.toContain("Hospital");
    });
  });

  describe("Checkbox state", () => {
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

      const columnButtons = wrapper
        .find(".dropdown-popper")
        .findAll("button")
        .filter((b) => b.findComponent(InputCheckboxIcon).exists());
      const checkedLabels = columnButtons
        .filter((b) => b.findComponent(InputCheckboxIcon).props("checked"))
        .map((b) => b.find("span").text().trim());
      expect(checkedLabels).toEqual(["Country", "Diagnosis"]);
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
      vi.advanceTimersByTime(600);
      await flushPromises();

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
