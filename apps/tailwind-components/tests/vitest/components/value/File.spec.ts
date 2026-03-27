import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import FileValue from "../../../../app/components/value/File.vue";
import type { fileValue, IColumn } from "../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "attachment",
  label: "Attachment",
  columnType: "FILE",
};

describe("value/File.vue", () => {
  it("renders a download link with the formatted size for file values", () => {
    const data: fileValue = {
      id: "file-1",
      filename: "report.pdf",
      extension: "pdf",
      url: "https://example.com/report.pdf",
      size: 1536,
    };

    const wrapper = mount(FileValue, {
      props: {
        metadata,
        data,
      },
    });

    const link = wrapper.find("a");

    expect(link.exists()).toBe(true);
    expect(link.text()).toBe("report.pdf");
    expect(link.attributes("href")).toBe("https://example.com/report.pdf");
    expect(link.attributes("download")).toBe("report.pdf");
    expect(wrapper.text()).toContain("(size: 1.5 KB)");
  });

  it("falls back to plain text when there is no downloadable url", () => {
    const data: fileValue = {
      id: "file-2",
      filename: "notes.txt",
      extension: "txt",
      url: "",
      size: 0,
    };

    const wrapper = mount(FileValue, {
      props: {
        metadata,
        data,
      },
    });

    expect(wrapper.find("a").exists()).toBe(false);
    expect(wrapper.text()).toContain("notes.txt");
    expect(wrapper.text()).toContain("(size: 0 B)");
  });

  it("shows default fallback values when file details are missing or invalid", () => {
    const myData = {
          id: "file-3",
          filename: "No file",
          extension: "",
          url: "",
          size: -1,
        } as fileValue
    const wrapper = mount(FileValue, {
      props: {
        metadata,
        data: myData,
      }
    });

    expect(wrapper.text()).toContain("No file");
    expect(wrapper.text()).toContain("(size: Unknown size)");
  });
});
