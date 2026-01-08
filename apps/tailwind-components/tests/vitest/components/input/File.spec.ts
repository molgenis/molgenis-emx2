import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import InputFile from "../../../../app/components/input/File.vue";

const wrapper = mount(InputFile, {
  props: {
    id: "files",
  },
});

describe("file", () => {
  it("input is empty by default", async () => {
    const inputValueContainer = wrapper.find(
      "div[data-elem='current-file-container']"
    );
    expect(
      (inputValueContainer as unknown as HTMLInputElement).childNodes
    ).toBeFalsy();
  });

  it("current value button is shown when file is imported and can be deleted", async () => {
    const input = wrapper.find('input[type="file"]');
    const testFile = new File(["test"], "test.txt", { type: "text/plain" });
    Object.defineProperty(input.element, "files", { value: [testFile] });

    await input.trigger("change");
    await wrapper.vm.$nextTick();

    const inputValueContainer = wrapper.find(
      "div[data-elem='current-file-container']"
    );
    // console.log(inputValueContainer.childNodes)

    // const fileInputBtn = wrapper.find("button[id='files-current-file']");
    // console.log(fileInputBtn)
    // await fileInputBtn.trigger("click")
    // expect(fileInputBtn.exists()).toBeFalsy();
  });
});
