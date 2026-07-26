import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DisplayRecord from "../../../../app/components/display/Record.vue";
import type {
  ITableMetaData,
  recordValue,
} from "../../../../../metadata-utils/src/types";

function birdsMetadata(): ITableMetaData {
  return {
    id: "birds",
    schemaId: "petstore",
    name: "birds",
    label: "Birds",
    tableType: "DATA",
    columns: [
      { id: "kind", label: "Kind", columnType: "STRING" },
      { id: "overview", label: "Overview", columnType: "SECTION" },
      { id: "name", label: "Name", columnType: "STRING" },
      { id: "details", label: "Details", columnType: "HEADING" },
      { id: "age", label: "Age", columnType: "INT" },
    ],
  };
}

const birdRow: recordValue = { kind: "Canary", name: "Tweety", age: 3 };

function mountRecord() {
  return mount(DisplayRecord, {
    props: { tableMetadata: birdsMetadata(), inputRowData: birdRow },
  });
}

describe("display/Record.vue", () => {
  it("groups the columns that follow each heading under that heading, ahead of a group for the columns before the first heading", () => {
    const wrapper = mountRecord();

    const groups = wrapper.findAll("div > ul");
    expect(
      groups.map((group) => group.findAll("li").map((item) => item.text()))
    ).toEqual([["KindCanary"], ["NameTweety"], ["Age3"]]);
  });

  it("renders no group ahead of the first heading when no column precedes it", () => {
    const metadata = birdsMetadata();
    metadata.columns = metadata.columns.slice(1);

    const wrapper = mount(DisplayRecord, {
      props: { tableMetadata: metadata, inputRowData: birdRow },
    });

    const groups = wrapper.findAll("div > ul");
    expect(
      groups.map((group) => group.findAll("li").map((item) => item.text()))
    ).toEqual([["NameTweety"], ["Age3"]]);
  });

  it("renders a SECTION one heading level above a HEADING", () => {
    const wrapper = mountRecord();

    const labels = wrapper.findAll("p");
    expect(labels.map((label) => label.text())).toEqual([
      "Overview",
      "Details",
    ]);
    expect(labels[0]!.classes()).toContain("text-heading-lg");
    expect(
      labels[1]!.classes().filter((name) => name.startsWith("text-heading-"))
    ).toEqual([]);
  });
});
