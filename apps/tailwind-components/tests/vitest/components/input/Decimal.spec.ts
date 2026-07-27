import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it } from "vitest";
import { nextTick } from "vue";
import InputDecimal from "../../../../app/components/input/Decimal.vue";

describe("input decimal", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(() => {
    wrapper = mount(InputDecimal, {
      props: {
        id: "test-decimal",
        modelValue: null,
        "onUpdate:modelValue": (value: unknown) =>
          wrapper.setProps({ modelValue: value }),
      },
    });
  });

  function lastEmittedValue() {
    return wrapper.emitted("update:modelValue")?.at(-1);
  }

  function fieldValue() {
    return (wrapper.get("input").element as HTMLInputElement).value;
  }

  it("keeps the fraction of a decimal value instead of truncating it", async () => {
    await wrapper.get("input").setValue("3.7");
    expect(lastEmittedValue()).toEqual(["3.7"]);
  });

  it("keeps the fraction visible in the field after the value is bound back", async () => {
    await wrapper.get("input").setValue("3.7");
    await nextTick();
    expect(fieldValue()).toBe("3.7");
  });

  it("passes a pasted comma value through unchanged so it can be rejected", async () => {
    await wrapper.get("input").setValue("2,75");
    expect(lastEmittedValue()).toEqual(["2,75"]);
  });

  it("shows the pasted comma value rather than a silently corrected one", async () => {
    await wrapper.get("input").setValue("2,75");
    await nextTick();
    expect(fieldValue()).toBe("2,75");
  });

  it("passes unparseable input through unchanged rather than emitting NaN", async () => {
    await wrapper.get("input").setValue("1.2.3");
    expect(lastEmittedValue()).toEqual(["1.2.3"]);
  });

  it("emits null when the field is cleared", async () => {
    await wrapper.get("input").setValue("3.7");
    await wrapper.get("input").setValue("");
    expect(lastEmittedValue()).toEqual([null]);
  });

  it("emits a zero value rather than treating it as cleared", async () => {
    await wrapper.get("input").setValue("0");
    expect(lastEmittedValue()).toEqual(["0"]);
  });
});
