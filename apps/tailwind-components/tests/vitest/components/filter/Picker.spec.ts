import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import Picker from "../../../../app/components/filter/Picker.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

vi.mock("#app", () => ({
  createError: (e: unknown) => e,
}));

const ontologyCol: IColumn = {
  id: "status",
  label: "Status",
  columnType: "ONTOLOGY",
  table: "MyTable",
  position: 0,
} as IColumn;

const stringCol: IColumn = {
  id: "notes",
  label: "Notes",
  columnType: "STRING",
  table: "MyTable",
  position: 1,
} as IColumn;

const boolCol: IColumn = {
  id: "active",
  label: "Active",
  columnType: "BOOL",
  table: "MyTable",
  position: 2,
} as IColumn;

const refCol: IColumn = {
  id: "category",
  label: "Category",
  columnType: "REF",
  table: "MyTable",
  refTableId: "Category",
  position: 3,
} as IColumn;

const headingCol: IColumn = {
  id: "section1",
  label: "Section",
  columnType: "HEADING",
  table: "MyTable",
  position: 4,
} as IColumn;

const mgCol: IColumn = {
  id: "mg_insertedOn",
  label: "Inserted on",
  columnType: "DATETIME",
  table: "MyTable",
  position: 5,
} as IColumn;

const allColumns: IColumn[] = [
  ontologyCol,
  stringCol,
  boolCol,
  refCol,
  headingCol,
  mgCol,
];

function mountPicker(
  overrides: {
    modelValue?: boolean;
    columns?: IColumn[];
    visibleFilterIds?: Set<string>;
  } = {}
) {
  return mount(Picker, {
    props: {
      modelValue: overrides.modelValue ?? true,
      columns: overrides.columns ?? allColumns,
      visibleFilterIds: overrides.visibleFilterIds ?? new Set(["status"]),
      schemaId: "pet-store",
      tableId: "MyTable",
    },
    attachTo: document.body,
  });
}

describe("Picker", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
  });

  it("renders modal when modelValue is true", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();
    expect(document.body.innerHTML).toContain("Customize filters");
    wrapper.unmount();
  });

  it("does not render modal content when modelValue is false", async () => {
    const wrapper = mountPicker({ modelValue: false });
    await nextTick();
    expect(document.body.innerHTML).not.toContain("Customize filters");
    wrapper.unmount();
  });

  it("shows countable columns (ONTOLOGY, BOOL) with checkboxes by default", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();
    const html = document.body.innerHTML;
    expect(html).toContain("Status");
    expect(html).toContain("Active");
    wrapper.unmount();
  });

  it("shows STRING columns by default", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();
    const html = document.body.innerHTML;
    expect(html).toContain("Notes");
    wrapper.unmount();
  });

  it("hides HEADING columns always", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();
    const html = document.body.innerHTML;
    expect(html).not.toContain("Section");
    wrapper.unmount();
  });

  it("hides mg_* columns by default", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();
    const html = document.body.innerHTML;
    expect(html).not.toContain("Inserted on");
    wrapper.unmount();
  });

  it("reveals STRING columns when search matches", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();

    const searchInput = document.body.querySelector(
      'input[type="search"]'
    ) as HTMLInputElement;
    searchInput.value = "notes";
    searchInput.dispatchEvent(new Event("input", { bubbles: true }));

    await new Promise((r) => setTimeout(r, 600));
    await nextTick();

    expect(document.body.innerHTML).toContain("Notes");
    wrapper.unmount();
  });

  it("shows checkbox checked for filters in visibleFilterIds on open", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status"]),
    });
    await nextTick();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const statusCheckbox = Array.from(checkboxes).find((cb) =>
      cb.closest("label")?.textContent?.includes("Status")
    );
    expect(statusCheckbox?.checked).toBe(true);
    wrapper.unmount();
  });

  it("shows checkbox unchecked for columns not in visibleFilterIds", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status"]),
    });
    await nextTick();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const activeCheckbox = Array.from(checkboxes).find((cb) =>
      cb.closest("label")?.textContent?.includes("Active")
    );
    expect(activeCheckbox?.checked).toBe(false);
    wrapper.unmount();
  });

  it("toggles local selection on checkbox click without emitting apply", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status"]),
    });
    await nextTick();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const activeCheckbox = Array.from(checkboxes).find((cb) =>
      cb.closest("label")?.textContent?.includes("Active")
    );

    expect(activeCheckbox?.checked).toBe(false);
    activeCheckbox?.click();
    await nextTick();

    expect(wrapper.emitted("apply")).toBeFalsy();

    const updatedCheckboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const updatedActive = Array.from(updatedCheckboxes).find((cb) =>
      cb.closest("label")?.textContent?.includes("Active")
    );
    expect(updatedActive?.checked).toBe(true);
    wrapper.unmount();
  });

  it("emits apply with selected IDs when Apply is clicked", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status"]),
    });
    await nextTick();

    const buttons = document.body.querySelectorAll("button");
    const applyButton = Array.from(buttons).find((b) =>
      b.textContent?.trim().includes("Apply")
    );
    applyButton?.click();
    await nextTick();

    expect(wrapper.emitted("apply")).toBeTruthy();
    const emittedSet = wrapper.emitted("apply")![0][0] as Set<string>;
    expect(emittedSet instanceof Set).toBe(true);
    expect(emittedSet.has("status")).toBe(true);
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")![0][0]).toBe(false);
    wrapper.unmount();
  });

  it("emits cancel and closes when Cancel is clicked", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status"]),
    });
    await nextTick();

    const buttons = document.body.querySelectorAll("button");
    const cancelButton = Array.from(buttons).find((b) =>
      b.textContent?.trim().includes("Cancel")
    );
    cancelButton?.click();
    await nextTick();

    expect(wrapper.emitted("cancel")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")![0][0]).toBe(false);
    wrapper.unmount();
  });

  it("reset restores default filter set without applying", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["active"]),
    });
    await nextTick();

    const buttons = document.body.querySelectorAll("button");
    const resetButton = Array.from(buttons).find((b) =>
      b.textContent?.trim().includes("Reset")
    );
    resetButton?.click();
    await nextTick();

    expect(wrapper.emitted("apply")).toBeFalsy();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const statusCheckbox = Array.from(checkboxes).find((cb) =>
      cb.closest("label")?.textContent?.includes("Status")
    );
    expect(statusCheckbox?.checked).toBe(true);
    wrapper.unmount();
  });

  it("local selection does not affect parent until Apply", async () => {
    const initialIds = new Set(["status"]);
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: initialIds,
    });
    await nextTick();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const activeCheckbox = Array.from(checkboxes).find((cb) =>
      cb.closest("label")?.textContent?.includes("Active")
    );
    activeCheckbox?.click();
    await nextTick();

    expect(initialIds.has("active")).toBe(false);
    expect(wrapper.emitted("apply")).toBeFalsy();
    wrapper.unmount();
  });

  it("shows REF columns with arrow indicator", async () => {
    const wrapper = mountPicker({ modelValue: true });
    await nextTick();

    const html = document.body.innerHTML;
    expect(html).toContain("Category");
    expect(html).toContain("→");
    wrapper.unmount();
  });

  it("Clear button deselects all filters without applying", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status", "active"]),
    });
    await nextTick();

    const buttons = document.body.querySelectorAll("button");
    const clearButton = Array.from(buttons).find((b) =>
      b.textContent?.trim().includes("Clear")
    );
    expect(clearButton).toBeTruthy();

    clearButton?.click();
    await nextTick();

    expect(wrapper.emitted("apply")).toBeFalsy();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const anyChecked = Array.from(checkboxes).some((cb) => cb.checked);
    expect(anyChecked).toBe(false);
    wrapper.unmount();
  });

  it("Select all button selects all selectable columns", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(),
    });
    await nextTick();

    const selectAllButton = Array.from(
      document.body.querySelectorAll("button")
    ).find((b) => b.textContent?.trim().includes("Select all"));
    expect(selectAllButton).toBeTruthy();

    selectAllButton?.click();
    await nextTick();

    expect(wrapper.emitted("apply")).toBeFalsy();

    const checkboxes = document.body.querySelectorAll(
      'input[type="checkbox"]'
    ) as NodeListOf<HTMLInputElement>;
    const allChecked = Array.from(checkboxes).every((cb) => cb.checked);
    expect(allChecked).toBe(true);
    expect(checkboxes.length).toBeGreaterThan(0);
    wrapper.unmount();
  });

  it("Clear then Apply emits apply with empty Set", async () => {
    const wrapper = mountPicker({
      modelValue: true,
      visibleFilterIds: new Set(["status", "active"]),
    });
    await nextTick();

    const clearButton = Array.from(
      document.body.querySelectorAll("button")
    ).find((b) => b.textContent?.trim().includes("Clear"));
    clearButton?.click();
    await nextTick();

    const applyButton = Array.from(
      document.body.querySelectorAll("button")
    ).find((b) => b.textContent?.trim().includes("Apply"));
    applyButton?.click();
    await nextTick();

    expect(wrapper.emitted("apply")).toBeTruthy();
    const emittedSet = wrapper.emitted("apply")![0][0] as Set<string>;
    expect(emittedSet instanceof Set).toBe(true);
    expect(emittedSet.size).toBe(0);
    wrapper.unmount();
  });
});
