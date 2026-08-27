import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataTable from "../../../../app/components/display/DataTable.vue";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../../../../app/types/display";

const ageColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

const resolved: ResolvedDisplay = {
  layout: "TABLE",
  titleTemplate: "${name}",
  detailColumns: [ageColumn],
};

describe("DataTable.vue", () => {
  it("renders one table row per record, with the title in the first cell", () => {
    const wrapper = mount(DataTable, {
      props: { rows, resolved },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    expect(bodyRows.length).toBe(2);
    expect(bodyRows[0].findAll("td")[0].text()).toBe("Tweety");
    expect(bodyRows[1].findAll("td")[0].text()).toBe("Sylvester");
  });

  it("renders detailColumns as the remaining columns, through value/EMX2.vue", () => {
    const wrapper = mount(DataTable, {
      props: { rows, resolved },
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

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(DataTable, {
      props: { rows, resolved, linkTo: (row: IRow) => `/records/${row.name}` },
    });

    const firstAnchor = wrapper.find("tbody tr td a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(DataTable, {
      props: { rows, resolved },
    });

    expect(wrapper.find("tbody tr td a").exists()).toBe(false);
  });
});
