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

describe("Enum input with isArray", () => {
  it("renders checkboxes instead of a listbox when isArray is set", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "enum-array",
        isArray: true,
        values: ["TAG_A", "TAG_B", "TAG_C"],
        modelValue: null,
      },
    });

    expect(wrapper.findAll('input[type="checkbox"]').length).toEqual(3);
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false);
  });

  it("marks the pre-existing array modelValue options as checked on edit", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "enum-array-edit",
        isArray: true,
        values: ["TAG_A", "TAG_B", "TAG_C"],
        modelValue: ["TAG_A", "TAG_C"],
      },
    });

    const checkedValues = wrapper
      .findAll('input[type="checkbox"]')
      .filter((checkbox) => (checkbox.element as HTMLInputElement).checked)
      .map((checkbox) => (checkbox.element as HTMLInputElement).value);
    expect(checkedValues).toEqual(["TAG_A", "TAG_C"]);
  });
});
