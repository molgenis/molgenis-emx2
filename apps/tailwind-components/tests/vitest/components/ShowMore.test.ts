import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { nextTick } from "vue";
import ShowMore from "../../../app/components/ShowMore.vue";

let observers = 0;
/** Re-measure, as a real resize would. Changing the stub alone leaves it stale. */
let resize = () => {};

// jsdom lays nothing out, so scrollHeight and clientHeight are both 0 and the
// component would never see an overflow. These stubs decide the answer instead.
function overflowing(yes: boolean) {
  Object.defineProperty(HTMLElement.prototype, "scrollHeight", {
    configurable: true,
    get: () => (yes ? 100 : 10),
  });
  Object.defineProperty(HTMLElement.prototype, "clientHeight", {
    configurable: true,
    get: () => 10,
  });
}

async function clamp(props: Record<string, unknown>, overflows = true) {
  overflowing(overflows);
  const wrapper = mount(ShowMore, {
    props,
    slots: { default: "some long content" },
  });
  await nextTick();
  return wrapper;
}

const lines = (w: ReturnType<typeof mount>) =>
  w.find("span span").attributes("style");
const labels = (w: ReturnType<typeof mount>) =>
  w.findAll("button").map((b) => b.text());

beforeEach(() => {
  observers = 0;
  resize = () => {};
  (globalThis as any).ResizeObserver = class {
    cb: () => void;
    constructor(cb: () => void) {
      this.cb = cb;
      observers += 1;
      resize = cb;
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

describe("ShowMore.vue", () => {
  it("clamps, and observes, only where the caller wants it collapsed", async () => {
    const unbounded = await clamp({ collapse: false });
    expect(unbounded.find("span span").classes()).not.toContain("show-more");
    expect(labels(unbounded)).toEqual([]);
    expect(observers).toBe(0);

    const bounded = await clamp({ maxLines: 2 });
    expect(lines(bounded)).toContain("--show-more-lines: 2");
    expect(observers).toBe(1);
    // Hiding is visual, so the content is still there to be read or indexed.
    expect(bounded.text()).toContain("some long content");
  });

  it("offers a control only while something is hidden", async () => {
    expect(labels(await clamp({ maxLines: 3 }, false))).toEqual([]);
    expect(labels(await clamp({ maxLines: 3 }))).toEqual(["show more"]);
  });

  it("grows the bound in steps, and never both grows and asks on one click", async () => {
    const w = await clamp({ maxLines: 3, lineStep: 5, hasMore: true });

    await w.find("button").trigger("click");
    expect(lines(w)).toContain("--show-more-lines: 8");
    expect(w.emitted("showMore")).toBeUndefined();

    overflowing(false);
    resize();
    await nextTick();
    await w.find("button").trigger("click");
    expect(w.emitted("showMore")).toHaveLength(1);
    expect(lines(w)).toContain("--show-more-lines: 8");
  });

  it("withholds collapse until nothing is left to reveal", async () => {
    const w = await clamp({ maxLines: 3, lineStep: 5 });

    await w.find("button").trigger("click");
    expect(labels(w)).toEqual(["show more"]);

    overflowing(false);
    resize();
    await nextTick();
    expect(labels(w)).toEqual(["show less"]);
  });

  it("lets a caller replace the control, keeping the label as the default", async () => {
    overflowing(true);
    const wrapper = mount(ShowMore, {
      props: { maxLines: 3 },
      slots: { default: "some long content", more: "read the rest" },
    });
    await nextTick();

    expect(labels(wrapper)).toEqual(["read the rest"]);
  });

  it("collapses to the starting bound, and offers to expand again", async () => {
    const w = await clamp({ maxLines: 3, lineStep: 5 });

    await w.find("button").trigger("click");
    overflowing(false);
    resize();
    await nextTick();
    await w.find("button").trigger("click");

    overflowing(true);
    resize();
    await nextTick();
    expect(lines(w)).toContain("--show-more-lines: 3");
    expect(labels(w)).toEqual(["show more"]);
  });
});
