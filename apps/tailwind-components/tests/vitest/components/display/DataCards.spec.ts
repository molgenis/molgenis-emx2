import { mount } from "@vue/test-utils";
import { describe, it, expect } from "vitest";
import DataCards from "../../../../app/components/display/DataCards.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

function col(overrides: Partial<IColumn>): IColumn {
  return {
    id: "test",
    label: "Test",
    columnType: "STRING",
    ...overrides,
  } as IColumn;
}

const ROLE_COLUMNS = [
  col({ id: "acronym", label: "Acronym", role: "TITLE" }),
  col({ id: "name", label: "Name", role: "SUBTITLE" }),
  col({ id: "description", label: "Description", role: "DESCRIPTION" }),
];

const ROW = {
  acronym: "AVOHILMO",
  name: "Register of Primary Health Care Visits",
  description: "A Finnish primary care register",
};

describe("display/DataCards.vue role-driven title and subtitle", () => {
  it("titles the card from the TITLE column when no rowLabelTemplate is given", () => {
    const wrapper = mount(DataCards, {
      props: { rows: [ROW], columns: ROLE_COLUMNS },
    });

    expect(wrapper.find("li .font-bold").text()).toBe("AVOHILMO");
  });

  it("renders the SUBTITLE column under the title", () => {
    const wrapper = mount(DataCards, {
      props: { rows: [ROW], columns: ROLE_COLUMNS },
    });

    expect(wrapper.find("li [data-testid='card-subtitle']").text()).toBe(
      "Register of Primary Health Care Visits"
    );
  });

  it("falls back to the key columns for the title when no roles are declared", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows: [{ pid: "AVOHILMO", name: "not the key" }],
        columns: [
          col({ id: "pid", label: "Pid", key: 1 }),
          col({ id: "name", label: "Name" }),
        ],
      },
    });

    expect(wrapper.find("li .font-bold").text()).toBe("AVOHILMO");
  });

  it("keeps rowLabelTemplate in charge of the title when one is given", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows: [ROW],
        columns: ROLE_COLUMNS,
        rowLabelTemplate: "${name}",
      },
    });

    expect(wrapper.find("li .font-bold").text()).toBe(
      "Register of Primary Health Care Visits"
    );
  });

  it("renders no subtitle element when no SUBTITLE column is declared", () => {
    const wrapper = mount(DataCards, {
      props: {
        rows: [{ id: "AVOHILMO" }],
        columns: [col({ id: "id", label: "Id", key: 1 })],
      },
    });

    expect(wrapper.find("[data-testid='card-subtitle']").exists()).toBe(false);
  });
});
