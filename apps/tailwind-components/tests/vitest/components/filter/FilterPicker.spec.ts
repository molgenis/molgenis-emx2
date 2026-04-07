import { describe, it, expect, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import FilterPicker from "../../../../app/components/filter/FilterPicker.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { ITreeNode, ITreeNodeState } from "../../../../types/types";
import { createMockUseFilters } from "../../fixtures/mockFilters";

describe("FilterPicker", () => {
  const mockColumns: IColumn[] = [
    { id: "heading1", label: "Demographics", columnType: "HEADING" },
    {
      id: "name",
      label: "Name",
      columnType: "STRING",
      description: "Patient full name",
    },
    { id: "age", label: "Age", columnType: "INT" },
    {
      id: "country",
      label: "Country",
      columnType: "ONTOLOGY",
      refSchemaId: "CatalogueOntologies",
      refTableId: "Countries",
      description: "Country of residence",
    },
    {
      id: "diagnosis",
      label: "Diagnosis",
      columnType: "ONTOLOGY_ARRAY",
      refSchemaId: "CatalogueOntologies",
      refTableId: "Diagnoses",
    },
    {
      id: "hospital",
      label: "Hospital",
      columnType: "REF",
      refSchemaId: "test",
      refTableId: "Hospital",
      description: "Primary hospital",
    },
    {
      id: "medications",
      label: "Medications",
      columnType: "REF_ARRAY",
      refSchemaId: "test",
      refTableId: "Medication",
    },
    { id: "weight", label: "Weight", columnType: "DECIMAL" },
    { id: "admissionDate", label: "Admission Date", columnType: "DATE" },
    { id: "notes", label: "Notes", columnType: "TEXT" },
    { id: "active", label: "Active", columnType: "BOOL" },
    { id: "mg_internal", label: "Internal Field", columnType: "STRING" },
    { id: "section1", label: "Section", columnType: "SECTION" },
  ];

  const inputSearchStub = {
    name: "InputSearch",
    template: `<input type="search" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
    props: ["modelValue"],
    emits: ["update:modelValue"],
  };

  const treeStub = {
    name: "Tree",
    template: `
      <div data-testid="tree">
        <div
          v-for="node in nodes"
          :key="node.name"
          :data-node-name="node.name"
        >
          <input
            type="checkbox"
            :name="node.name"
            :checked="modelValue.includes(node.name)"
            @change="$emit('update:modelValue', toggleNode(node.name))"
            :aria-label="node.label || node.name"
          />
          <span>{{ node.label || node.name }}</span>
          <div
            v-for="child in node.children || []"
            :key="child.name"
            :data-node-name="child.name"
          >
            <input
              type="checkbox"
              :name="child.name"
              :checked="modelValue.includes(child.name)"
              @change="$emit('update:modelValue', toggleNode(child.name))"
              :aria-label="child.label || child.name"
            />
            <span>{{ child.label || child.name }}</span>
          </div>
        </div>
      </div>
    `,
    props: ["nodes", "modelValue", "isMultiSelect", "id"],
    emits: ["update:modelValue"],
    methods: {
      toggleNode(name: string) {
        const current: string[] = (this as any).modelValue ?? [];
        return current.includes(name)
          ? current.filter((n: string) => n !== name)
          : [...current, name];
      },
    },
  };

  const modalStub = {
    name: "Modal",
    template: `
      <div>
        <slot />
        <slot name="footer" :hide="hide" />
      </div>
    `,
    props: ["visible", "title", "maxWidth"],
    emits: ["update:visible"],
    methods: {
      hide() {
        (this as any).$emit("update:visible", false);
      },
    },
  };

  function createMockFilters(
    columns: IColumn[] = mockColumns,
    visibleIds: string[] = ["country", "diagnosis"]
  ) {
    return createMockUseFilters({
      reactive: false,
      initialColumns: columns,
      initialVisibleIds: visibleIds,
    });
  }

  const defaultProps = { filters: createMockFilters() };

  async function openModal(wrapper: ReturnType<typeof mount>) {
    await wrapper.find("button[aria-haspopup='dialog']").trigger("click");
    await flushPromises();
  }

  const globalStubs = {
    Modal: modalStub,
    Tree: treeStub,
    InputSearch: inputSearchStub,
  };

  describe("Basic rendering", () => {
    it("renders Customize button", () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      expect(wrapper.text()).toContain("Customize");
    });

    it("shows Tree component after dialog opens", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      expect(wrapper.find('[data-testid="tree"]').exists()).toBe(true);
    });

    it("shows search input when dialog opens", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      expect(wrapper.find('input[type="search"]').exists()).toBe(true);
    });

    it("shows Save and Cancel buttons in footer", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      expect(wrapper.text()).toContain("Save");
      expect(wrapper.text()).toContain("Cancel");
    });

    it("shows Clear and Reset to defaults buttons in footer", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      expect(wrapper.text()).toContain("Clear");
      expect(wrapper.text()).toContain("Reset to defaults");
    });
  });

  describe("Column visibility in tree nodes", () => {
    it("passes selectable columns as tree nodes (age, country, diagnosis, active)", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const text = wrapper.text();
      expect(text).toContain("Age");
      expect(text).toContain("Country");
      expect(text).toContain("Diagnosis");
      expect(text).toContain("Active");
    });

    it("excludes STRING columns from tree when search is empty and showAll is off", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      const nodes = treeComponent.props("nodes") as ITreeNodeState[];
      const nodeNames = nodes.map((n) => n.name);
      expect(nodeNames).not.toContain("name");
      expect(nodeNames).not.toContain("notes");
    });

    it("does not pass HEADING, SECTION, mg_* or FILE columns to tree", async () => {
      const columnsWithFile: IColumn[] = [
        ...mockColumns,
        { id: "attachment", label: "Attachment", columnType: "FILE" },
      ];
      const wrapper = mount(FilterPicker, {
        props: { filters: createMockFilters(columnsWithFile) },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      const nodes = treeComponent.props("nodes") as ITreeNode[];
      const nodeNames = nodes.map((n) => n.name);
      expect(nodeNames).not.toContain("heading1");
      expect(nodeNames).not.toContain("section1");
      expect(nodeNames).not.toContain("mg_internal");
      expect(nodeNames).not.toContain("attachment");
    });

    it("includes REF and REF_ARRAY columns as expandable nodes in tree", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      const nodes = treeComponent.props("nodes") as ITreeNode[];
      const nodeNames = nodes.map((n) => n.name);
      expect(nodeNames).toContain("hospital");
      expect(nodeNames).toContain("medications");
    });

    it("shows STRING columns when showAll toggle is enabled", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const toggle = wrapper.find('input[id$="-show-all"]');
      await toggle.setValue(true);
      await flushPromises();

      const treeComponent = wrapper.findComponent(treeStub);
      const nodes = treeComponent.props("nodes") as ITreeNode[];
      const nodeNames = nodes.map((n) => n.name);
      expect(nodeNames).toContain("name");
      expect(nodeNames).toContain("notes");
    });
  });

  describe("Search functionality", () => {
    it("includes string columns in tree nodes when search matches their label", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("Name");
      await flushPromises();

      const treeComponent = wrapper.findComponent(treeStub);
      const nodes = treeComponent.props("nodes") as ITreeNode[];
      const nodeNames = nodes.map((n) => n.name);
      expect(nodeNames).toContain("name");
    });

    it("prunes tree to only matching columns when search is active", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("Country");
      await flushPromises();

      const treeComponent = wrapper.findComponent(treeStub);
      const nodes = treeComponent.props("nodes") as ITreeNodeState[];
      const nodeNames = nodes.map((n) => n.name);
      expect(nodeNames).toContain("country");
      expect(nodeNames).not.toContain("age");
      expect(nodeNames).not.toContain("diagnosis");
    });

    it("shows no results message when search has no matches", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const searchInput = wrapper.find('input[type="search"]');
      await searchInput.setValue("nonexistent");
      await flushPromises();

      expect(wrapper.text()).toContain("No columns match your search");
    });
  });

  describe("Selection state", () => {
    it("initializes localSelection from visibleFilterIds on open", async () => {
      const filters = createMockFilters(mockColumns, ["country", "diagnosis"]);
      const wrapper = mount(FilterPicker, {
        props: { filters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      expect(treeComponent.props("modelValue")).toContain("country");
      expect(treeComponent.props("modelValue")).toContain("diagnosis");
    });

    it("does not call toggleFilter when Tree emits changes (local only)", async () => {
      const mockFilters = createMockUseFilters({
        reactive: false,
        initialColumns: mockColumns,
        initialVisibleIds: ["country"],
      });
      const wrapper = mount(FilterPicker, {
        props: { filters: mockFilters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      await treeComponent.vm.$emit("update:modelValue", ["country", "age"]);
      await flushPromises();

      expect(mockFilters.toggleFilter).not.toHaveBeenCalled();
    });
  });

  describe("Save workflow", () => {
    it("calls toggleFilter for added ids on Save", async () => {
      const mockFilters = createMockUseFilters({
        reactive: false,
        initialColumns: mockColumns,
        initialVisibleIds: ["country"],
      });
      const wrapper = mount(FilterPicker, {
        props: { filters: mockFilters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      await treeComponent.vm.$emit("update:modelValue", ["country", "age"]);
      await flushPromises();

      const saveButton = wrapper
        .findAll("button")
        .find((b) => b.text() === "Save");
      await saveButton?.trigger("click");

      expect(mockFilters.toggleFilter).toHaveBeenCalledWith("age");
    });

    it("calls toggleFilter for removed ids on Save", async () => {
      const mockFilters = createMockUseFilters({
        reactive: false,
        initialColumns: mockColumns,
        initialVisibleIds: ["country", "age"],
      });
      const wrapper = mount(FilterPicker, {
        props: { filters: mockFilters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      await treeComponent.vm.$emit("update:modelValue", ["country"]);
      await flushPromises();

      const saveButton = wrapper
        .findAll("button")
        .find((b) => b.text() === "Save");
      await saveButton?.trigger("click");

      expect(mockFilters.toggleFilter).toHaveBeenCalledWith("age");
    });

    it("does not call toggleFilter on Cancel", async () => {
      const mockFilters = createMockUseFilters({
        reactive: false,
        initialColumns: mockColumns,
        initialVisibleIds: ["country"],
      });
      const wrapper = mount(FilterPicker, {
        props: { filters: mockFilters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const treeComponent = wrapper.findComponent(treeStub);
      await treeComponent.vm.$emit("update:modelValue", ["country", "age"]);
      await flushPromises();

      const cancelButton = wrapper
        .findAll("button")
        .find((b) => b.text() === "Cancel");
      await cancelButton?.trigger("click");

      expect(mockFilters.toggleFilter).not.toHaveBeenCalled();
    });
  });

  describe("Clear functionality", () => {
    it("sets local selection to empty array", async () => {
      const filters = createMockFilters(mockColumns, ["country", "diagnosis"]);
      const wrapper = mount(FilterPicker, {
        props: { filters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const clearButton = wrapper
        .findAll("button")
        .find((b) => b.text() === "Clear");
      await clearButton?.trigger("click");
      await flushPromises();

      const treeComponent = wrapper.findComponent(treeStub);
      expect(treeComponent.props("modelValue")).toEqual([]);
    });

    it("does not apply clear until Save is clicked", async () => {
      const mockFilters = createMockUseFilters({
        reactive: false,
        initialColumns: mockColumns,
        initialVisibleIds: ["country", "diagnosis"],
      });
      const wrapper = mount(FilterPicker, {
        props: { filters: mockFilters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const clearButton = wrapper
        .findAll("button")
        .find((b) => b.text() === "Clear");
      await clearButton?.trigger("click");

      expect(mockFilters.toggleFilter).not.toHaveBeenCalled();
    });
  });

  describe("Reset functionality", () => {
    it("shows Reset to defaults button", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      expect(wrapper.text()).toContain("Reset to defaults");
    });

    it("sets localSelection to computeDefaultFilters result on reset", async () => {
      const filters = createMockFilters(mockColumns, []);
      const wrapper = mount(FilterPicker, {
        props: { filters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const resetButton = wrapper
        .findAll("button")
        .find((b) => b.text().includes("Reset to defaults"));
      await resetButton?.trigger("click");
      await flushPromises();

      const treeComponent = wrapper.findComponent(treeStub);
      const selection = treeComponent.props("modelValue") as string[];
      expect(selection).toContain("country");
      expect(selection).toContain("diagnosis");
      expect(selection).toContain("active");
      expect(selection).not.toContain("age");
    });

    it("does not apply reset until Save is clicked", async () => {
      const mockFilters = createMockUseFilters({
        reactive: false,
        initialColumns: mockColumns,
        initialVisibleIds: ["country", "age", "weight"],
      });
      const wrapper = mount(FilterPicker, {
        props: { filters: mockFilters },
        global: { stubs: globalStubs },
      });

      await openModal(wrapper);

      const resetButton = wrapper
        .findAll("button")
        .find((b) => b.text().includes("Reset to defaults"));
      await resetButton?.trigger("click");

      expect(mockFilters.toggleFilter).not.toHaveBeenCalled();
    });
  });

  describe("Modal visibility", () => {
    it("closes modal by setting visible to false via model binding", async () => {
      const wrapper = mount(FilterPicker, {
        props: defaultProps,
        global: { stubs: globalStubs },
      });

      await wrapper.find("button[aria-haspopup='dialog']").trigger("click");
      await flushPromises();

      const modalComponent = wrapper.findComponent(modalStub);
      expect(modalComponent.props("visible")).toBe(true);

      await modalComponent.vm.$emit("update:visible", false);
      await flushPromises();

      expect(modalComponent.props("visible")).toBe(false);
    });
  });
});
