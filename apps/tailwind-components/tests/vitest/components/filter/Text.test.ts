import { describe, it, expect, vi, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../../types/filters";
import FilterText from "../../../../app/components/filter/Text.vue";

function column(columnType: IColumn["columnType"] = "STRING"): IColumn {
  return { id: "name", label: "Name", columnType } as IColumn;
}

function mountText(
  col: IColumn,
  modelValue: IFilterValue | undefined = undefined
) {
  return mount(FilterText, { props: { column: col, modelValue } });
}

function rowValues(wrapper: ReturnType<typeof mountText>) {
  return wrapper
    .findAll("input")
    .map((input) => (input.element as HTMLInputElement).value);
}

async function typeInRow(
  wrapper: ReturnType<typeof mountText>,
  index: number,
  text: string
) {
  await wrapper.findAll("input")[index]!.setValue(text);
}

function buttons(wrapper: ReturnType<typeof mountText>, name: string) {
  return wrapper.findAll("button").filter((button) => button.text() === name);
}

function lastEmit(wrapper: ReturnType<typeof mountText>) {
  const emitted = wrapper.emitted("update:modelValue");
  return emitted![emitted!.length - 1]![0];
}

describe("FilterText", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("shows one empty text row and an add button for a column without a filter", () => {
    const wrapper = mountText(column());
    expect(rowValues(wrapper)).toEqual([""]);
    expect(buttons(wrapper, "Add an additional item")).toHaveLength(1);
    expect(buttons(wrapper, "Remove item")).toHaveLength(0);
  });

  it("shows one row per value, each with a remove button once there are two", () => {
    const wrapper = mountText(column(), {
      operator: "like",
      value: ["dog", "cat"],
    });
    expect(rowValues(wrapper)).toEqual(["dog", "cat"]);
    expect(buttons(wrapper, "Remove item")).toHaveLength(2);
  });

  it("adds a row and emits the trimmed values after the debounce", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column());
    await typeInRow(wrapper, 0, " dog ");
    await buttons(wrapper, "Add an additional item")[0]!.trigger("click");
    expect(rowValues(wrapper)).toEqual([" dog ", ""]);
    await typeInRow(wrapper, 1, "cat dog");
    vi.advanceTimersByTime(500);
    expect(lastEmit(wrapper)).toEqual({
      operator: "like",
      value: ["dog", "cat dog"],
    });
  });

  it("keeps the values when an empty row is added and left empty", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column(), { operator: "like", value: ["dog"] });
    await buttons(wrapper, "Add an additional item")[0]!.trigger("click");
    vi.advanceTimersByTime(500);
    expect(rowValues(wrapper)).toEqual(["dog", ""]);
    expect(lastEmit(wrapper)).toEqual({ operator: "like", value: ["dog"] });
  });

  it("ignores empty and whitespace rows", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column(), {
      operator: "like",
      value: ["dog", "cat"],
    });
    await typeInRow(wrapper, 1, "   ");
    vi.advanceTimersByTime(500);
    expect(lastEmit(wrapper)).toEqual({ operator: "like", value: ["dog"] });
  });

  it("emits undefined when every row is empty", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column(), { operator: "like", value: ["dog"] });
    await typeInRow(wrapper, 0, "");
    vi.advanceTimersByTime(500);
    expect(lastEmit(wrapper)).toBeUndefined();
  });

  it("removing a row emits the remaining values", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column(), {
      operator: "like",
      value: ["dog", "cat"],
    });
    await buttons(wrapper, "Remove item")[0]!.trigger("click");
    vi.advanceTimersByTime(500);
    expect(rowValues(wrapper)).toEqual(["cat"]);
    expect(lastEmit(wrapper)).toEqual({ operator: "like", value: ["cat"] });
  });

  it("rapid typing: 3 keystrokes within 100ms produce one emit after the debounce", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column());
    await typeInRow(wrapper, 0, "a");
    vi.advanceTimersByTime(100);
    await typeInRow(wrapper, 0, "ab");
    vi.advanceTimersByTime(100);
    await typeInRow(wrapper, 0, "abc");
    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
    vi.advanceTimersByTime(500);
    expect(wrapper.emitted("update:modelValue")).toEqual([
      [{ operator: "like", value: ["abc"] }],
    ]);
  });

  it("empties every row when the filter is removed from outside", async () => {
    const wrapper = mountText(column(), {
      operator: "like",
      value: ["dog", "cat"],
    });
    await wrapper.setProps({ modelValue: undefined });
    expect(rowValues(wrapper)).toEqual([""]);
  });

  it.each(["EMAIL", "HYPERLINK_ARRAY", "TEXT"] as const)(
    "%s column rows are plain text inputs",
    (columnType) => {
      const wrapper = mountText(column(columnType));
      expect(wrapper.find("input").attributes("type")).toBe("text");
    }
  );

  it("a UUID column keeps the single search box and emits a like list of one", async () => {
    vi.useFakeTimers();
    const wrapper = mountText(column("UUID"));
    expect(wrapper.find('input[type="search"]').exists()).toBe(true);
    expect(buttons(wrapper, "Add an additional item")).toHaveLength(0);
    await wrapper.find('input[type="search"]').setValue("abc");
    vi.advanceTimersByTime(500);
    expect(lastEmit(wrapper)).toEqual({ operator: "like", value: ["abc"] });
  });
});
