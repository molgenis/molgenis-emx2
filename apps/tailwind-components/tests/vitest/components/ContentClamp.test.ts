import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { nextTick } from "vue";
import ContentClamp from "../../../app/components/ContentClamp.vue";

const clamped = (wrapper: ReturnType<typeof mount>) =>
  wrapper.find("span span");

const isClamped = (wrapper: ReturnType<typeof mount>) =>
  clamped(wrapper).classes().includes("content-clamp");

/**
 * jsdom lays nothing out, so scrollHeight and clientHeight are both 0 and the
 * component would never see an overflow. These stubs decide the answer instead,
 * which is the part of the measurement worth pinning in a unit test.
 */
function stubOverflow(overflowing: boolean) {
  Object.defineProperty(HTMLElement.prototype, "scrollHeight", {
    configurable: true,
    get: () => (overflowing ? 100 : 10),
  });
  Object.defineProperty(HTMLElement.prototype, "clientHeight", {
    configurable: true,
    get: () => 10,
  });
}

const mountClamp = (props: Record<string, unknown>) =>
  mount(ContentClamp, { props, slots: { default: "some long content" } });

let observersCreated = 0;

beforeEach(() => {
  observersCreated = 0;
  (globalThis as any).ResizeObserver = class {
    cb: () => void;
    constructor(cb: () => void) {
      this.cb = cb;
      observersCreated += 1;
    }
    observe() {
      this.cb();
    }
    disconnect() {}
  };
});

afterEach(() => {
  delete (HTMLElement.prototype as any).scrollHeight;
  delete (HTMLElement.prototype as any).clientHeight;
});

describe("ContentClamp.vue", () => {
  it("does not clamp when no line bound is given", async () => {
    stubOverflow(true);
    const wrapper = mountClamp({});
    await nextTick();

    expect(isClamped(wrapper)).toBe(false);
    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("observes a clamped block, and only a clamped one", async () => {
    stubOverflow(true);
    mountClamp({});
    await nextTick();

    // A table page holds hundreds of unclamped lists; none should cost an observer.
    expect(observersCreated).toBe(0);

    mountClamp({ maxLines: 3 });
    await nextTick();

    expect(observersCreated).toBe(1);
  });

  it("bounds by lines, and keeps the content in the DOM", async () => {
    stubOverflow(true);
    const wrapper = mountClamp({ maxLines: 2 });
    await nextTick();

    expect(isClamped(wrapper)).toBe(true);
    expect(clamped(wrapper).attributes("style")).toContain(
      "--content-clamp-lines: 2"
    );
    // Hiding is visual, so the text is still there to be read or indexed.
    expect(wrapper.text()).toContain("some long content");
  });

  it("offers no control when the content already fits", async () => {
    stubOverflow(false);
    const wrapper = mountClamp({ maxLines: 3 });
    await nextTick();

    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("reveals in line steps rather than all at once", async () => {
    stubOverflow(true);
    const wrapper = mountClamp({ maxLines: 3, lineStep: 5 });
    await nextTick();

    expect(wrapper.find("button").text()).toBe("show more");

    await wrapper.find("button").trigger("click");
    expect(clamped(wrapper).attributes("style")).toContain(
      "--content-clamp-lines: 8"
    );

    await wrapper.find("button").trigger("click");
    expect(clamped(wrapper).attributes("style")).toContain(
      "--content-clamp-lines: 13"
    );
  });

  it("never shows both controls at once", async () => {
    stubOverflow(true);
    const wrapper = mountClamp({ maxLines: 3, lineStep: 5 });
    await nextTick();

    await wrapper.find("button").trigger("click");

    // Still overflowing, so there is more to reveal and nothing to collapse yet.
    const labels = wrapper.findAll("button").map((b) => b.text());
    expect(labels).toEqual(["show more"]);
  });

  it("returns to the starting bound, and offers to expand again", async () => {
    stubOverflow(true);
    const wrapper = mountClamp({ maxLines: 3, lineStep: 5 });
    await nextTick();

    // Reveal until nothing is left, which is when collapsing is offered.
    await wrapper.find("button").trigger("click");
    stubOverflow(false);
    await wrapper.find("button").trigger("click");
    await nextTick();
    expect(wrapper.find("button").text()).toBe("show less");

    // Collapsing restores the original bound, so the content overflows again.
    stubOverflow(true);
    await wrapper.find("button").trigger("click");
    await nextTick();

    expect(clamped(wrapper).attributes("style")).toContain(
      "--content-clamp-lines: 3"
    );
    expect(wrapper.findAll("button").map((b) => b.text())).toEqual([
      "show more",
    ]);
  });

  it("keeps offering, and withholds collapse, while the caller holds content back", async () => {
    stubOverflow(false);
    const wrapper = mount(ContentClamp, {
      props: { maxLines: 3, hasMore: true },
      slots: { default: "some long content" },
    });
    await nextTick();

    // Nothing overflows, but the caller has more it has not rendered yet.
    expect(wrapper.find("button").text()).toBe("show more");

    await wrapper.find("button").trigger("click");
    expect(wrapper.emitted("showMore")).toHaveLength(1);
    expect(wrapper.findAll("button").map((b) => b.text())).toEqual([
      "show more",
    ]);
  });
});
