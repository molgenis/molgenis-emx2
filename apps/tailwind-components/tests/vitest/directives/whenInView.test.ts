import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { whenInView } from "../../../app/directives/whenInView";

class FakeIntersectionObserver {
  static instances: FakeIntersectionObserver[] = [];
  disconnected = false;
  targets: Element[] = [];
  constructor(
    public callback: (entries: { isIntersecting: boolean }[]) => void,
    public options?: IntersectionObserverInit
  ) {
    FakeIntersectionObserver.instances.push(this);
  }
  observe(target: Element) {
    this.targets.push(target);
  }
  unobserve() {}
  disconnect() {
    this.disconnected = true;
  }
  takeRecords() {
    return [];
  }
}

function host(binding: unknown) {
  return mount(
    {
      template: `<div v-when-in-view="binding" />`,
      props: { binding: { type: null, required: false } },
    },
    {
      props: { binding },
      global: { directives: { "when-in-view": whenInView } },
    }
  );
}

describe("v-when-in-view", () => {
  beforeEach(() => {
    FakeIntersectionObserver.instances = [];
    vi.stubGlobal("IntersectionObserver", FakeIntersectionObserver);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("watches half of the element for a caller that passes only a handler", () => {
    const seen: number[] = [];
    host(() => seen.push(1));

    const [observer] = FakeIntersectionObserver.instances;
    expect(observer!.options).toEqual({ root: null, threshold: 0.5 });

    observer!.callback([{ isIntersecting: true }]);
    observer!.callback([{ isIntersecting: false }]);
    expect(seen).toEqual([1]);
  });

  test("watches what the caller asks for when it passes options", () => {
    host([
      () => {},
      { root: null, rootMargin: "0px 0px -80% 0px", threshold: 0 },
    ]);

    expect(FakeIntersectionObserver.instances[0]!.options).toEqual({
      root: null,
      rootMargin: "0px 0px -80% 0px",
      threshold: 0,
    });
  });

  test("builds no observer at all for a caller that switches it off", () => {
    host(null);

    expect(FakeIntersectionObserver.instances).toEqual([]);
  });

  test("stops watching an element that leaves the page", () => {
    const wrapper = host(() => {});
    const [observer] = FakeIntersectionObserver.instances;
    expect(observer!.disconnected).toBe(false);

    wrapper.unmount();
    expect(observer!.disconnected).toBe(true);
  });
});
