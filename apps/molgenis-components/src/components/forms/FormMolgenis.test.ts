import { describe, expect, test, vi } from "vitest";
import { mount } from "@vue/test-utils";
import FormMolgenis from "./FormMolgenis.vue";

describe("FormMolgenis", () => {
  test("calls the submit handler once per form submit", async () => {
    const onSubmit = vi.fn();
    const wrapper = mount(FormMolgenis, { props: { onSubmit } });

    await wrapper.find("form").trigger("submit");

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });
});
