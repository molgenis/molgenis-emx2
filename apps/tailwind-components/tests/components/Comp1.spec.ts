import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";

import Comp1 from "@/components/Comp1.vue";

describe("Comp1", () => {
  it("is a Vue instance", () => {
    const wrapper = mount(Comp1);
    expect(wrapper.vm).toBeTruthy();
  });
});
