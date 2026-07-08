import { mount } from "@vue/test-utils";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import TokenLabel from "../../../app/components/TokenLabel.vue";

const mockCopy = vi.fn();

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    useClipboard: () => ({
      copy: mockCopy,
      copied: { value: false },
      isSupported: { value: true },
    }),
  };
});

describe("TokenLabel", () => {
  beforeEach(() => {
    mockCopy.mockReset();
    mockCopy.mockResolvedValue(undefined);
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renders the token name", () => {
    const wrapper = mount(TokenLabel, {
      props: { tokenName: "bg-button-primary" },
    });
    expect(wrapper.text()).toContain("bg-button-primary");
  });

  it("calls copy with the token name when clicked", async () => {
    const wrapper = mount(TokenLabel, {
      props: { tokenName: "bg-button-primary" },
    });
    await wrapper.find("button").trigger("click");
    expect(mockCopy).toHaveBeenCalledWith("bg-button-primary");
  });

  it("uses the default aria-label referencing 'token name'", () => {
    const wrapper = mount(TokenLabel, {
      props: { tokenName: "text-title" },
    });
    expect(wrapper.find("button").attributes("aria-label")).toContain(
      "text-title"
    );
  });

  it("accepts a custom aria-label prop", () => {
    const wrapper = mount(TokenLabel, {
      props: { tokenName: "ArrowDown", ariaLabel: "Copy icon name ArrowDown" },
    });
    expect(wrapper.find("button").attributes("aria-label")).toBe(
      "Copy icon name ArrowDown"
    );
  });

  it("shows Copied affordance after click and hides it after 1500ms", async () => {
    const wrapper = mount(TokenLabel, {
      props: { tokenName: "bg-button-primary" },
    });

    const copiedSpan = wrapper.find("[aria-live='polite']");
    expect(copiedSpan.classes()).toContain("opacity-0");

    await wrapper.find("button").trigger("click");
    await wrapper.vm.$nextTick();
    expect(copiedSpan.classes()).toContain("opacity-100");

    vi.advanceTimersByTime(1500);
    await wrapper.vm.$nextTick();
    expect(copiedSpan.classes()).toContain("opacity-0");
  });
});
