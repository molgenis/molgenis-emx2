import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { InputBoolean } from "#components";

const wrapper = mount(InputBoolean);

describe("input boolean", () => {
  it("Use Yes/No as default labels", () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.props("trueLabel")).toBe("Yes");
    expect(wrapper.props("falseLabel")).toBe("No");
  });

  it("has correct options", () => {
    const altWrapper = mount(InputBoolean, {
      props: {
        trueLabel: "A",
        falseLabel: "B",
      },
    });

    expect(altWrapper.props("trueLabel")).toBe("A");
    expect(altWrapper.props("falseLabel")).toBe("B");
  });
});
