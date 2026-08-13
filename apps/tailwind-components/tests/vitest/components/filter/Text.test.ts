import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../../types/filters";
import FilterText from "../../../../app/components/filter/Text.vue";
import InputSearch from "../../../../app/components/input/Search.vue";

function stringColumn(): IColumn {
  return {
    id: "name",
    label: "Name",
    columnType: "STRING",
  } as IColumn;
}

function mountText(
  column: IColumn,
  modelValue: IFilterValue | undefined = undefined
) {
  return mount(FilterText, {
    props: { column, modelValue },
  });
}

describe("FilterText", () => {
  describe("string-like types", () => {
    it("renders search input for STRING column type", () => {
      const wrapper = mountText(stringColumn());
      const input = wrapper.find('input[type="search"]');
      expect(input.exists()).toBe(true);
    });

    it("renders InputSearch component for accessibility", () => {
      const wrapper = mountText(stringColumn());
      const inputSearch = wrapper.findComponent({ name: "InputSearch" });
      expect(inputSearch.exists()).toBe(true);
    });

    it("search input shows current like filter value", () => {
      const wrapper = mountText(stringColumn(), {
        operator: "like",
        value: "fluffy",
      });
      const input = wrapper.find('input[type="search"]');
      expect((input.element as HTMLInputElement).value).toBe("fluffy");
    });

    it("emits like filter on text input after debounce", async () => {
      vi.useFakeTimers();
      const wrapper = mountText(stringColumn());
      const input = wrapper.find('input[type="search"]');
      const el = input.element as HTMLInputElement;
      el.value = "hello";
      await input.trigger("input");
      vi.advanceTimersByTime(500);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toEqual({ operator: "like", value: "hello" });
      vi.useRealTimers();
    });

    it("rapid typing: 3 keystrokes within 100ms produce only one emit after 500ms", async () => {
      vi.useFakeTimers();
      const wrapper = mountText(stringColumn());
      const input = wrapper.find('input[type="search"]');
      const el = input.element as HTMLInputElement;

      el.value = "a";
      await input.trigger("input");
      vi.advanceTimersByTime(100);

      el.value = "ab";
      await input.trigger("input");
      vi.advanceTimersByTime(100);

      el.value = "abc";
      await input.trigger("input");

      expect(wrapper.emitted("update:modelValue")).toBeFalsy();

      vi.advanceTimersByTime(500);

      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted!.length).toBe(1);
      expect(emitted![0][0]).toEqual({ operator: "like", value: "abc" });
      vi.useRealTimers();
    });

    it("H7: input value clears when modelValue is removed (filter removed from sidebar)", async () => {
      const wrapper = mountText(stringColumn(), {
        operator: "like",
        value: "asdf",
      });
      const inputBefore = wrapper.find('input[type="search"]');
      expect((inputBefore.element as HTMLInputElement).value).toBe("asdf");

      await wrapper.setProps({ modelValue: undefined });
      await wrapper.vm.$nextTick();

      const inputAfter = wrapper.find('input[type="search"]');
      expect((inputAfter.element as HTMLInputElement).value).toBe("");
    });

    it("emits undefined when text input is cleared", async () => {
      vi.useFakeTimers();
      const wrapper = mountText(stringColumn(), {
        operator: "like",
        value: "hello",
      });
      const input = wrapper.find('input[type="search"]');
      const el = input.element as HTMLInputElement;
      el.value = "";
      await input.trigger("input");
      vi.advanceTimersByTime(500);
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toBeUndefined();
      vi.useRealTimers();
    });
  });

  describe("does not render Tree or FilterRange", () => {
    it("does not render Tree or FilterRange for string types", () => {
      const wrapper = mountText(stringColumn());
      expect(wrapper.findComponent({ name: "Tree" }).exists()).toBe(false);
      expect(wrapper.findComponent({ name: "FilterRange" }).exists()).toBe(
        false
      );
    });

    it("renders exactly one InputSearch", () => {
      const wrapper = mountText(stringColumn());
      const allSearchInputs = wrapper.findAllComponents(InputSearch);
      expect(allSearchInputs).toHaveLength(1);
    });
  });
});
