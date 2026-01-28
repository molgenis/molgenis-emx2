import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import RecordView from "../../../../app/components/display/RecordView.vue";
import type { ITableMetaData } from "../../../../../metadata-utils/src/types";

describe("RecordView", () => {
  const minimalMetadata: ITableMetaData = {
    id: "Test",
    schemaId: "test",
    name: "Test",
    label: "Test",
    tableType: "DATA",
    columns: [
      { id: "name", label: "Name", columnType: "STRING" },
      { id: "age", label: "Age", columnType: "INT" },
    ],
  };

  const metadataWithSections: ITableMetaData = {
    id: "Test",
    schemaId: "test",
    name: "Test",
    label: "Test",
    tableType: "DATA",
    columns: [
      { id: "orphan", label: "Orphan Field", columnType: "STRING" },
      { id: "section1", label: "Section One", columnType: "SECTION" },
      {
        id: "field1",
        label: "Field 1",
        columnType: "STRING",
        section: "section1",
      },
      {
        id: "heading1",
        label: "Heading One",
        columnType: "HEADING",
        section: "section1",
      },
      {
        id: "field2",
        label: "Field 2",
        columnType: "STRING",
        heading: "heading1",
      },
    ],
  };

  const row = {
    name: "Test Name",
    age: 42,
    orphan: "Orphan Value",
    field1: "Field 1 Value",
    field2: "Field 2 Value",
  };

  it("renders orphan columns without heading", () => {
    const wrapper = mount(RecordView, {
      props: {
        metadata: minimalMetadata,
        row,
      },
    });

    expect(wrapper.text()).toContain("Name");
    expect(wrapper.text()).toContain("Test Name");
    expect(wrapper.text()).toContain("Age");
    expect(wrapper.text()).toContain("42");
  });

  it("renders section headings with correct styles", () => {
    const wrapper = mount(RecordView, {
      props: {
        metadata: metadataWithSections,
        row,
      },
    });

    expect(wrapper.text()).toContain("Section One");
  });

  it("groups columns by section", () => {
    const wrapper = mount(RecordView, {
      props: {
        metadata: metadataWithSections,
        row,
      },
    });

    expect(wrapper.text()).toContain("Orphan Field");
    expect(wrapper.text()).toContain("Field 1");
    expect(wrapper.text()).toContain("Field 2");
  });

  it("renders header slot content", () => {
    const wrapper = mount(RecordView, {
      props: {
        metadata: minimalMetadata,
        row,
      },
      slots: {
        header: "<div class='test-header'>Header Content</div>",
      },
    });

    expect(wrapper.find(".test-header").exists()).toBe(true);
    expect(wrapper.text()).toContain("Header Content");
  });

  it("renders footer slot content", () => {
    const wrapper = mount(RecordView, {
      props: {
        metadata: minimalMetadata,
        row,
      },
      slots: {
        footer: "<div class='test-footer'>Footer Content</div>",
      },
    });

    expect(wrapper.find(".test-footer").exists()).toBe(true);
    expect(wrapper.text()).toContain("Footer Content");
  });

  it("respects showEmpty prop", () => {
    const metadataWithEmptyColumn: ITableMetaData = {
      ...minimalMetadata,
      columns: [
        ...minimalMetadata.columns,
        { id: "empty", label: "Empty Field", columnType: "STRING" },
      ],
    };

    const rowWithEmpty = { ...row, empty: null };

    const wrapperHidden = mount(RecordView, {
      props: {
        metadata: metadataWithEmptyColumn,
        row: rowWithEmpty,
        showEmpty: false,
      },
    });

    expect(wrapperHidden.text()).not.toContain("Empty Field");

    const wrapperShown = mount(RecordView, {
      props: {
        metadata: metadataWithEmptyColumn,
        row: rowWithEmpty,
        showEmpty: true,
      },
    });

    expect(wrapperShown.text()).toContain("Empty Field");
  });
});
