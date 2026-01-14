import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import InputFile from "../../../../app/components/input/File.vue";

const wrapper = mount(InputFile, {
  props: {
    id: "test-file",
    modelValue: null,
  },
});

describe("input file", () => {
  it("should show an empty input when modelValue is null", () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find("input").attributes("value")).toBe(undefined);
  });

  it("should render the correct modelValue filename", async () => {
    const file = new File(["file content"], "test-document.txt", {
      type: "text/plain",
    });
    await wrapper.setProps({ modelValue: file });
    expect(
      wrapper.find("button[data-elem='current-value-btn'] span").text()
    ).toBe("test-document.txt");
  });
});
