import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Decimal from "../../../../app/components/input/Decimal.vue";

function mountWithVModel(initial: string | number | null = null) {
  const wrapper = mount(Decimal, {
    props: {
      id: "decimal-input",
      modelValue: initial,
      "onUpdate:modelValue": (value: string | number | null | undefined) =>
        wrapper.setProps({ modelValue: value }),
    },
  });
  return wrapper;
}

async function typeChars(
  wrapper: ReturnType<typeof mountWithVModel>,
  text: string
) {
  const input = wrapper.get("input");
  for (const char of text) {
    const current = (input.element as HTMLInputElement).value;
    await input.trigger("keypress", {
      key: char,
      which: char.charCodeAt(0),
      keyCode: char.charCodeAt(0),
    });
    await input.setValue(current + char);
  }
}

describe("InputDecimal", () => {
  it("emits the decimal value when the user types a number, a dot and another number", async () => {
    const wrapper = mountWithVModel();

    await typeChars(wrapper, "1.5");

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    expect(emitted!.at(-1)).toEqual([1.5]);
    expect(wrapper.props("modelValue")).toBe(1.5);
  });

  it("emits multi-digit decimal values without truncating the fraction", async () => {
    const wrapper = mountWithVModel();

    await typeChars(wrapper, "12.34");

    expect(wrapper.emitted("update:modelValue")!.at(-1)).toEqual([12.34]);
  });

  it("parses a comma as decimal separator", async () => {
    const wrapper = mountWithVModel();

    await wrapper.get("input").setValue("3,75");

    expect(wrapper.emitted("update:modelValue")!.at(-1)).toEqual([3.75]);
  });

  it("keeps the fraction when flipping the sign with the minus key", async () => {
    const wrapper = mountWithVModel(2.5);
    const input = wrapper.get("input");
    (input.element as HTMLInputElement).value = "2.5";

    await input.trigger("keypress", { key: "-", which: 45, keyCode: 45 });

    expect(wrapper.emitted("update:modelValue")!.at(-1)).toEqual([-2.5]);
  });

  it("emits null when the input is cleared", async () => {
    const wrapper = mountWithVModel(1.5);

    await wrapper.get("input").setValue("");

    expect(wrapper.emitted("update:modelValue")!.at(-1)).toEqual([null]);
  });
});
