import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Table from "../../../../../app/components/display/records/Table.vue";
import ValueEMX2 from "../../../../../app/components/value/EMX2.vue";
import type { IColumn, IRow } from "../../../../../../metadata-utils/src/types";

const ageColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

describe("records/Table.vue", () => {
  it("renders one table row per record, with the title in the first cell", () => {
    const wrapper = mount(Table, {
      props: { rows, titleTemplate: "${name}", columns: [ageColumn] },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    expect(bodyRows.length).toBe(2);
    expect(bodyRows[0].findAll("td")[0].find("div").text()).toBe("Tweety");
    expect(bodyRows[1].findAll("td")[0].find("div").text()).toBe("Sylvester");
  });

  it("renders columns as the remaining columns, through value/EMX2.vue", () => {
    const wrapper = mount(Table, {
      props: { rows, titleTemplate: "${name}", columns: [ageColumn] },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    const firstRowCells = bodyRows[0].findAll("td");
    expect(firstRowCells.length).toBe(2);
    expect(firstRowCells[1].find("div").text()).toBe("3");

    const cellComponents = wrapper.findAllComponents(ValueEMX2);
    expect(cellComponents.length).toBe(2);
    expect(cellComponents[0].props("metadata")).toEqual(ageColumn);
    expect(cellComponents[0].props("data")).toBe(3);
  });

  it("wraps the title in a real <a href> when linkTo is passed", () => {
    const wrapper = mount(Table, {
      props: {
        rows,
        titleTemplate: "${name}",
        columns: [ageColumn],
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
    });

    const firstAnchor = wrapper.find("tbody tr td a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(Table, {
      props: { rows, titleTemplate: "${name}", columns: [ageColumn] },
    });

    expect(wrapper.find("tbody tr td a").exists()).toBe(false);
  });

  it("renders no Title column when titleTemplate is absent or empty", () => {
    for (const titleTemplate of [undefined, ""]) {
      const wrapper = mount(Table, {
        props: {
          rows,
          titleTemplate,
          columns: [ageColumn],
          linkTo: (row: IRow) => `/records/${row.name}`,
        },
      });

      const headers = wrapper.find("thead").findAll("th");
      expect(headers.map((th) => th.text())).toEqual(["Age"]);
      const cells = wrapper.find("tbody").findAll("tr")[0].findAll("td");
      expect(cells.length).toBe(1);
      expect(cells[0].text()).toContain("Age");
      expect(wrapper.find("tbody tr td a").exists()).toBe(false);
    }
  });

  it("renders the Title column when titleTemplate is set", () => {
    const wrapper = mount(Table, {
      props: { rows, titleTemplate: "${name}", columns: [ageColumn] },
    });

    expect(wrapper.find("thead").findAll("th")[0].text()).toBe("Title");
  });

  it("repeats each column's label on its cell, for the stacked mobile layout", () => {
    const wrapper = mount(Table, {
      props: { rows, titleTemplate: "${name}", columns: [ageColumn] },
    });

    const firstDataRow = wrapper.find("tbody").findAll("tr")[0];
    const cells = firstDataRow.findAll("td");
    expect(cells[0].text()).toContain("Title");
    expect(cells[1].text()).toContain("Age");
  });
});
