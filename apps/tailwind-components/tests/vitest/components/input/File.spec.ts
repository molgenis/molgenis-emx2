import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import InputFile from "../../../../app/components/input/File.vue";

const wrapper = mount(InputFile, {
  props: {
    id: "test-file",
    modelValue: null,
  },
});

describe("input file", () => {
  beforeEach(() => {
    vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:test-url");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });
  it("should show an empty input when modelValue is null", () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find("input").attributes("value")).toBe(undefined);
  });

  it("should render the correct modelValue filename", async () => {
    const file = new window.File(["test"], "test-document.txt", {
      type: "text/plain",
    });
    await wrapper.setProps({ modelValue: file });
    expect(
      wrapper.find("button[data-elem='current-value-btn'] span").text()
    ).toBe("test-document.txt");
  });

  it("should show a link to file value in the component", async () => {
    const file = new Blob(["file content"], {
      type: "text/plain",
    });
    Object.defineProperty(file, "url", {
      value: "http://example.com/test-document.txt",
    });
    await wrapper.setProps({ modelValue: file });
    const link = wrapper.find("a");

    expect(link.exists()).toBe(true);
    expect(link.attributes("href")).toMatch(
      "http://example.com/test-document.txt"
    );
    expect(link.attributes("href")).toBe("http://fake.url");
  });

  it("should not show a link when modelValue is null", async () => {
    await wrapper.setProps({ modelValue: null });
    const link = wrapper.find("a");
    expect(link.exists()).toBe(false);
  });

  it("should show a link to download the file", async () => {
    await wrapper.setProps({
      modelValue: {
        id: "1",
        filename: "test-document.txt",
        url: "http://example.com/test-document.txt",
      },
    });
    const link = wrapper.find("a");

    expect(link.exists()).toBe(true);
    expect(link.attributes("href")).toBe(
      "http://example.com/test-document.txt"
    );
  });

  it("should open link in new tab/window", async () => {
    await wrapper.setProps({
      modelValue: {
        id: "1",
        filename: "test-document.txt",
        url: "http://example.com/test-document.txt",
      },
    });
    const link = wrapper.find("a");

    expect(link.attributes("target")).toBe("_blank");
    expect(link.attributes("rel")).toBe("noopener noreferrer");
  });
});
