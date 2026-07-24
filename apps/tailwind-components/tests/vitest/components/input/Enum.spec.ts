import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import InputEnum from "../../../../app/components/input/Enum.vue";

describe("Enum input", () => {
  it("shows the row's pre-existing modelValue in the toggle on edit", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "enum-edit",
        values: ["A+", "A-", "B+"],
        modelValue: "B+",
        placeholder: "Select blood type",
      },
    });

    const toggle = wrapper.find('button[role="combobox"]');
    expect(toggle.text()).toContain("B+");
  });

  it("marks the pre-existing modelValue option as aria-selected on edit", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "enum-edit-aria",
        values: ["A+", "A-", "B+"],
        modelValue: "B+",
        placeholder: "Select blood type",
      },
    });

    const selected = wrapper
      .findAll('li[role="option"]')
      .find((li) => li.text().includes("B+"));
    expect(selected?.attributes("aria-selected")).toEqual("true");
  });
});
