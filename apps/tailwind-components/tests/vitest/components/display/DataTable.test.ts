import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataTable from "../../../../app/components/display/DataTable.vue";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../../../app/types/display";

const nameColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
  key: 1,
};

const ageColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const columns: IColumn[] = [nameColumn, ageColumn];

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

const displayConfig: DisplayConfig = { layout: "TABLE" };

describe("DataTable.vue", () => {
  it("renders one table row per record, with the title in the first cell", () => {
    const wrapper = mount(DataTable, {
      props: { rows, columns, displayConfig },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    expect(bodyRows.length).toBe(2);
    expect(bodyRows[0].findAll("td")[0].text()).toBe("Tweety");
    expect(bodyRows[1].findAll("td")[0].text()).toBe("Sylvester");
  });

  it("renders detailColumns as the remaining columns, through value/EMX2.vue", () => {
    const wrapper = mount(DataTable, {
      props: { rows, columns, displayConfig },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    const firstRowCells = bodyRows[0].findAll("td");
    expect(firstRowCells.length).toBe(2);
    expect(firstRowCells[1].text()).toBe("3");

    const cellComponents = wrapper.findAllComponents(ValueEMX2);
    expect(cellComponents.length).toBe(2);
    expect(cellComponents[0].props("metadata")).toEqual(ageColumn);
    expect(cellComponents[0].props("data")).toBe(3);
  });

  it("renders from rows and displayConfig alone, with no columns prop, when titleTemplate is given explicitly", () => {
    const wrapper = mount(DataTable, {
      props: {
        rows,
        displayConfig: { layout: "TABLE", titleTemplate: "${name}" },
      },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    expect(bodyRows[0].findAll("td")[0].text()).toBe("Tweety");
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(DataTable, {
      props: {
        rows,
        columns,
        displayConfig,
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const firstAnchor = wrapper.find("tbody tr td a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataTable, {
      props: { rows, columns, displayConfig },
    });

    expect(wrapper.find("tbody tr td a").exists()).toBe(false);
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(DataTable, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        displayConfig: { layout: "TABLE", titleTemplate: "" },
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const firstCell = wrapper.find("tbody tr td");
    expect(firstCell.text()).toBe("");
    const anchor = wrapper.find("tbody tr td a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
  });

  it("forwards maxLines, renderLimit, truncate and hideListSeparator to value/EMX2.vue unchanged", () => {
    const wrapper = mount(DataTable, {
      props: {
        rows,
        columns,
        displayConfig,
        maxLines: 2,
        renderLimit: 5,
        truncate: false,
        hideListSeparator: true,
      },
    });

    const cellComponent = wrapper.findComponent(ValueEMX2);
    expect(cellComponent.props("maxLines")).toBe(2);
    expect(cellComponent.props("renderLimit")).toBe(5);
    expect(cellComponent.props("truncate")).toBe(false);
    expect(cellComponent.props("hideListSeparator")).toBe(true);
  });
});
