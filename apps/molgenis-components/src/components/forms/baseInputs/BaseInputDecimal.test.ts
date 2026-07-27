import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, test } from "vitest";
import BaseInputDecimal from "./BaseInputDecimal.vue";

describe("BaseInputDecimal", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(() => {
    wrapper = mount(BaseInputDecimal, { props: { id: "decimal" } });
  });

  function lastEmittedValue() {
    return wrapper.emitted("update:modelValue")?.at(-1);
  }

  test("it should emit a decimal value unchanged", async () => {
    await wrapper.get("input").setValue("2.75");
    expect(lastEmittedValue()).toEqual(["2.75"]);
  });

  test("it should emit a comma value unchanged instead of multiplying it by removing the comma", async () => {
    await wrapper.get("input").setValue("2,75");
    expect(lastEmittedValue()).toEqual(["2,75"]);
  });

  test("it should emit null when the field is cleared", async () => {
    await wrapper.get("input").setValue("2.75");
    await wrapper.get("input").setValue("");
    expect(lastEmittedValue()).toEqual([null]);
  });
});
