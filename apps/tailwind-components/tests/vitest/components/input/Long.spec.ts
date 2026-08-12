import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it } from "vitest";
import InputLong from "../../../../app/components/input/Long.vue";
import { dispatchKeyPress } from "../../fixtures/keyPress";

const COMMA_KEY_CODE = 44;
const PERIOD_KEY_CODE = 46;
const DIGIT_SEVEN_KEY_CODE = 55;

describe("input long", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(() => {
    wrapper = mount(InputLong, {
      props: {
        id: "test-long",
        modelValue: null,
        "onUpdate:modelValue": (value: unknown) =>
          wrapper.setProps({ modelValue: value }),
      },
    });
  });

  it("cancels a typed comma so it can never enter the field", () => {
    const event = dispatchKeyPress(
      wrapper.get("input").element,
      COMMA_KEY_CODE
    );
    expect(event.defaultPrevented).toBe(true);
  });

  it("cancels a typed period because a long has no fraction", () => {
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
