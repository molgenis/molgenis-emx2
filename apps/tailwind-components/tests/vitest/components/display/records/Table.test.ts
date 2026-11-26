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

const nameColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
};

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

describe("records/Table.vue", () => {
  it("renders one table row per record, with the title in the first cell", () => {
    const wrapper = mount(Table, {
      props: { rows, titleColumn: nameColumn, columns: [ageColumn] },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    expect(bodyRows.length).toBe(2);
    expect(bodyRows[0].findAll("td")[0].find("div").text()).toBe("Tweety");
    expect(bodyRows[1].findAll("td")[0].find("div").text()).toBe("Sylvester");
  });

  it("renders columns as the remaining columns, through value/EMX2.vue", () => {
    const wrapper = mount(Table, {
      props: { rows, titleColumn: nameColumn, columns: [ageColumn] },
    });

    const bodyRows = wrapper.find("tbody").findAll("tr");
    const firstRowCells = bodyRows[0].findAll("td");
    expect(firstRowCells.length).toBe(2);
    expect(firstRowCells[1].find("div").text()).toBe("3");

    const ageCells = wrapper
      .findAllComponents(ValueEMX2)
      .filter((cell) => cell.props("metadata").id === "age");
    expect(ageCells.length).toBe(2);
    expect(ageCells[0].props("metadata")).toEqual(ageColumn);
    expect(ageCells[0].props("data")).toBe(3);
  });

  it("wraps the title in a NuxtLink when linkTo is passed", () => {
    const wrapper = mount(Table, {
      props: {
        rows,
        titleColumn: nameColumn,
        columns: [ageColumn],
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ["to"],
            template: '<a data-nuxt-link :href="to"><slot /></a>',
          },
        },
      },
    });

    const firstAnchor = wrapper.find("tbody tr td a");
    expect(firstAnchor.attributes("href")).toBe("/records/Tweety");
    expect(firstAnchor.attributes()).toHaveProperty("data-nuxt-link");
    expect(firstAnchor.text()).toBe("Tweety");
    expect(firstAnchor.classes()).toContain("underline");
  });

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(Table, {
      props: { rows, titleColumn: nameColumn, columns: [ageColumn] },
    });

    expect(wrapper.find("tbody tr td a").exists()).toBe(false);
  });

  it("renders no title column when titleColumn is not set", () => {
    const wrapper = mount(Table, {
      props: {
        rows,
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
  });

  it("heads the title column with that column's own label, and does not repeat it among the other columns", () => {
    const wrapper = mount(Table, {
      props: {
        rows,
        titleColumn: nameColumn,
        columns: [nameColumn, ageColumn],
      },
    });

    expect(
      wrapper
        .find("thead")
        .findAll("th")
        .map((th) => th.text())
    ).toEqual(["Name", "Age"]);
    expect(wrapper.find("tbody").findAll("tr")[0].findAll("td").length).toBe(2);
  });

  it("falls back to the column id as header when the title column has no label", () => {
    const wrapper = mount(Table, {
      props: {
        rows,
        titleColumn: { id: "name", columnType: "STRING" } as IColumn,
        columns: [ageColumn],
      },
    });

    expect(wrapper.find("thead").findAll("th")[0].text()).toBe("name");
  });

  it("renders the title cell through value/EMX2.vue", () => {
    const wrapper = mount(Table, {
      props: { rows, titleColumn: nameColumn, columns: [ageColumn] },
    });

    const titleCell = wrapper.findAllComponents(ValueEMX2)[0];
    expect(titleCell.props("metadata")).toEqual(nameColumn);
    expect(titleCell.props("data")).toBe("Tweety");
  });

  it("repeats each column's label on its cell, for the stacked mobile layout", () => {
    const wrapper = mount(Table, {
      props: { rows, titleColumn: nameColumn, columns: [ageColumn] },
    });

    const firstDataRow = wrapper.find("tbody").findAll("tr")[0];
    const cells = firstDataRow.findAll("td");
    expect(cells[0].text()).toContain("Name");
    expect(cells[1].text()).toContain("Age");
  });
});
