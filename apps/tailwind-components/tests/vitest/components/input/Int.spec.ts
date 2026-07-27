import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it } from "vitest";
import { nextTick } from "vue";
import InputArray from "../../../../app/components/input/Array.vue";
import InputInt from "../../../../app/components/input/Int.vue";
import { dispatchKeyPress } from "../../fixtures/keyPress";

const COMMA_KEY_CODE = 44;
const PERIOD_KEY_CODE = 46;
const DIGIT_SEVEN_KEY_CODE = 55;

describe("input int", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(() => {
    wrapper = mount(InputInt, {
      props: {
        id: "test-int",
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

  it("passes a pasted comma value through unchanged so it can be rejected", async () => {
    await wrapper.get("input").setValue("2,75");
    expect(lastEmittedValue()).toEqual(["2,75"]);
  });

  it("shows the pasted comma value rather than a silently corrected one", async () => {
    await wrapper.get("input").setValue("2,75");
    await nextTick();
    expect(fieldValue()).toBe("2,75");
  });

  it("passes a pasted fractional value through unchanged rather than truncating it", async () => {
    await wrapper.get("input").setValue("2.75");
    expect(lastEmittedValue()).toEqual(["2.75"]);
  });

  it("keeps a comma value unchanged when the minus key flips its sign", async () => {
    await wrapper.get("input").setValue("2,75");
    await wrapper.get("input").trigger("keypress", { keyCode: 45 });
    expect(lastEmittedValue()).toEqual(["-2,75"]);
  });

  it("emits null when a filled field is cleared", async () => {
    await wrapper.get("input").setValue("37");
    await wrapper.get("input").setValue("");
    expect(lastEmittedValue()).toEqual([null]);
  });

  it("emits an integer for a filled field", async () => {
    await wrapper.get("input").setValue("37");
    expect(lastEmittedValue()).toEqual([37]);
  });

  it("emits a lone minus sign so a negative number stays typeable", async () => {
    await wrapper.get("input").setValue("-");
    expect(lastEmittedValue()).toEqual(["-"]);
  });

  it("cancels a typed comma so it can never enter the field", () => {
    const event = dispatchKeyPress(
      wrapper.get("input").element,
      COMMA_KEY_CODE
    );
    expect(event.defaultPrevented).toBe(true);
  });

  it("cancels a typed period because an integer has no fraction", () => {
    const event = dispatchKeyPress(
      wrapper.get("input").element,
      PERIOD_KEY_CODE
    );
    expect(event.defaultPrevented).toBe(true);
  });

  it("lets a typed digit through", () => {
    const event = dispatchKeyPress(
      wrapper.get("input").element,
      DIGIT_SEVEN_KEY_CODE
    );
    expect(event.defaultPrevented).toBe(false);
  });
});

describe("input int array", () => {
  it("emits null for a cleared item instead of an empty string", async () => {
    const wrapper = mount(InputArray, {
      props: {
        id: "test-int-array",
        type: "INT_ARRAY",
        errorMessage: undefined,
        modelValue: [37, 42],
      },
    });

    await wrapper.get("#test-int-array_0").setValue("");

    expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual([[null, 42]]);
  });
});
