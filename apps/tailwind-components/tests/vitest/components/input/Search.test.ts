import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import Search from "../../../../app/components/input/Search.vue";

describe("Search", () => {
  it("H7: DOM input clears when modelValue transitions from string to undefined", async () => {
    const wrapper = mount(Search, { props: { modelValue: "asdf" } });
    const inputEl = wrapper.get("input").element as HTMLInputElement;
    inputEl.value = "asdf";
    expect(inputEl.value).toBe("asdf");
    await wrapper.setProps({ modelValue: undefined });
    await nextTick();
    expect(inputEl.value).toBe("");
  });

  it("H7: DOM input .value property clears when modelValue transitions from string to undefined (simulates user-typed state)", async () => {
    const wrapper = mount(Search, { props: { modelValue: "srd" } });
    const inputEl = wrapper.get("input").element as HTMLInputElement;
    inputEl.value = "srd";
    expect(inputEl.value).toBe("srd");
    await wrapper.setProps({ modelValue: undefined });
    await nextTick();
    expect(inputEl.value).toBe("");
  });
});
