import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DataPairs from "../../../../app/components/display/DataPairs.vue";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";

const ageColumn: IColumn = { id: "age", label: "Age", columnType: "INT" };
const bioColumn: IColumn = { id: "bio", label: "Bio", columnType: "TEXT" };

const row: IRow = { name: "Tweety", age: 3, bio: "A canary" };

describe("DataPairs.vue", () => {
  it("renders one dt/dd pair per column, through value/EMX2.vue, in the folded (default) shape", () => {
    const wrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row },
    });

    expect(wrapper.find("dl").classes()).toContain("grid");
    expect(wrapper.find("dl").classes()).toContain("gap-1");

    const pairs = wrapper.findAll("dl > div");
    expect(pairs.length).toBe(2);
    expect(pairs[0].find("dt").text()).toBe("Age");
    expect(pairs[0].find("dd").text()).toBe("3");

    const cellComponents = wrapper.findAllComponents(ValueEMX2);
    expect(cellComponents.length).toBe(2);
    expect(cellComponents[0].props("metadata")).toEqual(ageColumn);
    expect(cellComponents[0].props("data")).toBe(3);
  });

  it("renders dt and dd as direct grid children, with no wrapper div, in the wide shape", () => {
    const wrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row, wide: true },
    });

    expect(wrapper.find("dl > div").exists()).toBe(false);
    const children = wrapper.find("dl").element.children;
    expect(Array.from(children).map((el) => el.tagName)).toEqual([
      "DT",
      "DD",
      "DT",
      "DD",
    ]);

    const cellComponents = wrapper.findAllComponents(ValueEMX2);
    expect(cellComponents.length).toBe(2);
    expect(cellComponents[1].props("metadata")).toEqual(bioColumn);
    expect(cellComponents[1].props("data")).toBe("A canary");
  });

  it("caps a narrow-type dd's width in the wide shape, and never caps in the folded shape", () => {
    const wideWrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row, wide: true },
    });
    const wideDds = wideWrapper.find("dl").findAll("dd");
    expect(wideDds[0].classes()).toContain("max-w-xs");
    expect(wideDds[1].classes()).not.toContain("max-w-xs");

    const foldedWrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row },
    });
    const foldedDds = foldedWrapper.find("dl").findAll("dd");
    expect(foldedDds[0].classes()).not.toContain("max-w-xs");
  });

  it("picks the wide-structure container-query band from the column count", () => {
    // jsdom never evaluates a container query, so this asserts the classes
    // that carry the behaviour. Two different counts are checked so the
    // band lookup itself, not just its presence, is covered.
    const twoWrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row, wide: true },
    });
    const twoDl = twoWrapper.find("dl").classes();
    expect(twoDl).toContain("@container");
    expect(twoDl).toContain("@sm:grid-cols-[repeat(2,minmax(160px,1fr))]");
    expect(twoDl).toContain("@sm:grid-flow-col");
    expect(twoDl).toContain("@sm:grid-rows-2");

    const fourColumns = [
      { id: "a", label: "A", columnType: "INT" },
      { id: "b", label: "B", columnType: "INT" },
      { id: "c", label: "C", columnType: "INT" },
      { id: "d", label: "D", columnType: "INT" },
    ];
    const fourWrapper = mount(DataPairs, {
      props: {
        columns: fourColumns,
        row: { a: 1, b: 2, c: 3, d: 4 },
        wide: true,
      },
    });
    const fourDl = fourWrapper.find("dl").classes();
    expect(fourDl).toContain(
      "@[50.5rem]:grid-cols-[repeat(4,minmax(160px,1fr))]"
    );
    expect(fourDl).not.toContain("@sm:grid-cols-[repeat(2,minmax(160px,1fr))]");
  });

  it("marks the dl for its fold-band's hide-when-empty rule by default in the wide shape, and drops the marker when hideEmpty is false", () => {
    // jsdom evaluates neither the @container max-width rule nor :has(), so
    // this can only assert the hook the scoped CSS rule keys off (the
    // data-fold-columns attribute), not the visual hide/show outcome.
    const defaultWrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row, wide: true },
    });
    expect(defaultWrapper.find("dl").attributes("data-fold-columns")).toBe("2");

    const disabledWrapper = mount(DataPairs, {
      props: {
        columns: [ageColumn, bioColumn],
        row,
        wide: true,
        hideEmpty: false,
      },
    });
    expect(
      disabledWrapper.find("dl").attributes("data-fold-columns")
    ).toBeUndefined();
  });

  it("never marks for the hide-when-empty rule with a single column, or in the folded shape", () => {
    const singleColumnWrapper = mount(DataPairs, {
      props: { columns: [ageColumn], row, wide: true },
    });
    expect(
      singleColumnWrapper.find("dl").attributes("data-fold-columns")
    ).toBeUndefined();

    const foldedWrapper = mount(DataPairs, {
      props: { columns: [ageColumn, bioColumn], row },
    });
    expect(
      foldedWrapper.find("dl").attributes("data-fold-columns")
    ).toBeUndefined();
  });

  it("forwards maxLines, renderLimit, truncate and hideListSeparator to value/EMX2.vue unchanged, in both shapes", () => {
    const props = {
      columns: [ageColumn],
      row,
      maxLines: 2,
      renderLimit: 5,
      truncate: false,
      hideListSeparator: true,
    };

    const foldedCell = mount(DataPairs, { props }).findComponent(ValueEMX2);
    expect(foldedCell.props("maxLines")).toBe(2);
    expect(foldedCell.props("renderLimit")).toBe(5);
    expect(foldedCell.props("truncate")).toBe(false);
    expect(foldedCell.props("hideListSeparator")).toBe(true);

    const wideCell = mount(DataPairs, {
      props: { ...props, wide: true },
    }).findComponent(ValueEMX2);
    expect(wideCell.props("maxLines")).toBe(2);
    expect(wideCell.props("renderLimit")).toBe(5);
    expect(wideCell.props("truncate")).toBe(false);
    expect(wideCell.props("hideListSeparator")).toBe(true);
  });
});
