import { describe, it, expect, beforeEach, vi } from "vitest";
import { ref } from "vue";
import { useColumnResize } from "../../../app/composables/useColumnResize";

describe("useColumnResize", () => {
  let container: any;

  beforeEach(() => {
    container = ref({
      getBoundingClientRect: () => ({ left: 100 }),
      scrollLeft: 0,
    });

    // mock RAF
    vi.stubGlobal("requestAnimationFrame", (cb: any) => {
      cb();
      return 1;
    });

    vi.stubGlobal("cancelAnimationFrame", vi.fn());
  });

  it("initializes column widths", () => {
    const { columnWidths, setInitialWidths } = useColumnResize(container);

    setInitialWidths([{ id: "name" }, { id: "age" }]);

    expect(columnWidths.value.name).toBe(240);
    expect(columnWidths.value.age).toBe(240);
  });

  it("does not override existing width", () => {
    const { columnWidths, setInitialWidths } = useColumnResize(container);

    columnWidths.value.name = 500;

    setInitialWidths([{ id: "name" }]);

    expect(columnWidths.value.name).toBe(500);
  });

  it("starts resizing correctly", () => {
    const { columnWidths, startResize, guideX } = useColumnResize(container);

    columnWidths.value.name = 200;

    startResize({ clientX: 300 } as MouseEvent, "name");

    // 300 - container.left(100)
    expect(guideX.value).toBe(200);
  });

  it("updates guide position on mouse move", () => {
    const { columnWidths, startResize, guideX } = useColumnResize(container);

    columnWidths.value.name = 200;

    startResize({ clientX: 300 } as MouseEvent, "name");

    window.dispatchEvent(new MouseEvent("mousemove", { clientX: 350 }));

    expect(guideX.value).toBe(250);
  });

  it("commits new column width on mouseup", () => {
    const { columnWidths, startResize } = useColumnResize(container);

    columnWidths.value.name = 200;

    startResize({ clientX: 300 } as MouseEvent, "name");

    window.dispatchEvent(new MouseEvent("mousemove", { clientX: 350 }));

    window.dispatchEvent(new MouseEvent("mouseup"));

    expect(columnWidths.value.name).toBe(250);
  });

  it("respects minimum width", () => {
    const { columnWidths, startResize } = useColumnResize(container);

    columnWidths.value.name = 200;

    startResize({ clientX: 300 } as MouseEvent, "name");

    window.dispatchEvent(new MouseEvent("mousemove", { clientX: 0 }));

    window.dispatchEvent(new MouseEvent("mouseup"));

    expect(columnWidths.value.name).toBeGreaterThanOrEqual(80);
  });

  it("removes guide after resize ends", () => {
    const { columnWidths, startResize, guideX } = useColumnResize(container);

    columnWidths.value.name = 200;

    startResize({ clientX: 300 } as MouseEvent, "name");

    window.dispatchEvent(new MouseEvent("mouseup"));

    expect(guideX.value).toBe(null);
  });
});
