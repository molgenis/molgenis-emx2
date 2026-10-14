import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import InputListbox from "../../../../app/components/input/Listbox.vue";

const OPTIONS = [
  { value: "group_a", label: "Group A" },
  { value: "group_b", label: "Group B" },
  { value: "group_c", label: "Group C" },
];

const mountListbox = (props: Record<string, unknown> = {}) =>
  mount(InputListbox, {
    attachTo: document.body,
    props: { id: "lb", options: OPTIONS, ...props },
  });

type Wrapper = ReturnType<typeof mountListbox>;

const toggle = (wrapper: Wrapper) => wrapper.find('button[role="combobox"]');
const optionEls = (wrapper: Wrapper) =>
  wrapper.findAll('ul[role="listbox"] li[role="option"]');
const chips = (wrapper: Wrapper) =>
  wrapper.findAll('button[aria-label^="Remove "]');
const chipLabels = (wrapper: Wrapper) =>
  chips(wrapper).map((chip) => chip.text());

async function open(wrapper: Wrapper) {
  await toggle(wrapper).trigger("click");
  await flushPromises();
}

async function pick(wrapper: Wrapper, index: number) {
  await optionEls(wrapper)[index]!.trigger("click");
  await flushPromises();
}

enableAutoUnmount(afterEach);

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = "";
});

describe("Listbox, single mode", () => {
  it("toggle carries the combobox aria attributes and points at the listbox", () => {
    const wrapper = mountListbox();
    const listbox = wrapper.find('ul[role="listbox"]');

    expect(toggle(wrapper).attributes()).toMatchObject({
      "aria-controls": listbox.attributes("id"),
      "aria-required": "false",
      "aria-expanded": "false",
      "aria-haspopup": "listbox",
    });
  });

  it("opens on click and closes after a pick", async () => {
    const wrapper = mountListbox();

    await open(wrapper);
    expect(toggle(wrapper).attributes("aria-expanded")).toBe("true");

    await pick(wrapper, 1);
    expect(toggle(wrapper).attributes("aria-expanded")).toBe("false");
  });

  it("has no placeholder option; the placeholder shows only in an empty toggle", async () => {
    const wrapper = mountListbox();

    expect(optionEls(wrapper).map((o) => o.text())).toEqual([
      "Group A",
      "Group B",
      "Group C",
    ]);
    expect(toggle(wrapper).text()).toBe("Select an option");

    await open(wrapper);
    await pick(wrapper, 1);
    expect(toggle(wrapper).text()).toBe("Group B");
  });

  it("marks only the stored option as aria-selected", () => {
    const wrapper = mountListbox({ modelValue: OPTIONS[1] });

    expect(
      optionEls(wrapper).map((o) => o.attributes("aria-selected"))
    ).toEqual(["false", "true", "false"]);
  });

  it("emits the picked option of a labelled list", async () => {
    const wrapper = mountListbox();
    await open(wrapper);
    await pick(wrapper, 2);

    expect(wrapper.emitted("update:modelValue")).toEqual([[OPTIONS[2]]]);
  });

  it("emits the picked value of a plain list", async () => {
    const wrapper = mountListbox({ options: ["x", "y", "z"] });
    await open(wrapper);
    await pick(wrapper, 1);

    expect(wrapper.emitted("update:modelValue")).toEqual([["y"]]);
  });

  it("seeds the selection from the value prop", async () => {
    const wrapper = mountListbox({ options: ["x", "y", "z"], value: "z" });
    await flushPromises();

    expect(toggle(wrapper).text()).toBe("z");
    expect(wrapper.emitted("update:modelValue")).toEqual([["z"]]);
  });

  it.each([
    ["Enter", "Enter"],
    ["Space", " "],
    ["Tab", "Tab"],
  ])("%s on an option picks it and closes the panel", async (_name, key) => {
    const wrapper = mountListbox({ options: ["x", "y", "z"] });
    await open(wrapper);
    await optionEls(wrapper)[2]!.trigger("keydown", { key });

    expect(wrapper.emitted("update:modelValue")).toEqual([["z"]]);
    expect(toggle(wrapper).attributes("aria-expanded")).toBe("false");
  });

  it("opening focuses the selected option", async () => {
    const wrapper = mountListbox({ modelValue: OPTIONS[1] });
    await open(wrapper);

    expect(document.activeElement).toBe(optionEls(wrapper)[1]!.element);
  });

  it("opening focuses the first option when nothing is selected", async () => {
    const wrapper = mountListbox();
    await open(wrapper);

    expect(document.activeElement).toBe(optionEls(wrapper)[0]!.element);
  });

  it("ArrowDown on the toggle opens the panel", async () => {
    const wrapper = mountListbox();
    await toggle(wrapper).trigger("keydown", { key: "ArrowDown" });
    await flushPromises();

    expect(toggle(wrapper).attributes("aria-expanded")).toBe("true");
  });

  it("shows the no-results message when there are no options", async () => {
    const wrapper = mountListbox({ options: [] });
    await open(wrapper);

    expect(optionEls(wrapper)).toHaveLength(0);
    expect(wrapper.text()).toContain("No results found");
  });

  it("passes describedBy to the toggle", () => {
    const wrapper = mountListbox({ describedBy: "lb-description lb-error" });

    expect(toggle(wrapper).attributes("aria-describedby")).toBe(
      "lb-description lb-error"
    );
  });

  it("emits focus when focus enters and blur when it leaves", async () => {
    const wrapper = mountListbox();
    await flushPromises();

    (toggle(wrapper).element as HTMLButtonElement).focus();
    await flushPromises();
    expect(wrapper.emitted("focus")).toHaveLength(1);

    (toggle(wrapper).element as HTMLButtonElement).blur();
    await flushPromises();
    expect(wrapper.emitted("blur")).toHaveLength(1);
  });

  it("emits search when the user types and never filters the options itself", async () => {
    const wrapper = mountListbox({ enableSearch: true });
    await open(wrapper);
    await wrapper.find('input[type="search"]').setValue("zzz");

    expect(wrapper.emitted("search")?.at(-1)).toEqual(["zzz"]);
    expect(optionEls(wrapper)).toHaveLength(3);
  });
  it.each([
    ["an option", {}, "li"],
    ["an option, multiple", { multiple: true }, "li"],
    ["the search input", { enableSearch: true }, "input"],
    [
      "the search input, multiple",
      { enableSearch: true, multiple: true },
      "input",
    ],
    ["the panel after a mouse pick, multiple", { multiple: true }, "panel"],
  ])(
    "Escape on %s closes the panel without a pick",
    async (_where, props, at) => {
      const wrapper = mountListbox(props);
      await open(wrapper);
      if (at === "panel") await pick(wrapper, 1);
      const target =
        at === "li"
          ? optionEls(wrapper)[0]!
          : at === "input"
          ? wrapper.find('input[type="search"]')
          : wrapper.find("#listbox-lb-panel");
      await target.trigger("keydown", { key: "Escape" });

      expect(toggle(wrapper).attributes("aria-expanded")).toBe("false");
      expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
        at === "panel" ? 1 : 0
      );
    }
  );

  it.each([
    ["ArrowDown", "single", {}],
    ["ArrowDown", "multiple", { multiple: true }],
    ["Tab", "single", {}],
    ["Tab", "multiple", { multiple: true }],
  ])(
    "%s in the search input moves focus to the first option, %s mode",
    async (key, _mode, props) => {
      const wrapper = mountListbox({ enableSearch: true, ...props });
      await open(wrapper);
      const input = wrapper.find('input[type="search"]');
      (input.element as HTMLInputElement).focus();
      await input.trigger("keydown", { key });
      await flushPromises();

      expect(document.activeElement).toBe(optionEls(wrapper)[0]!.element);
      expect(toggle(wrapper).attributes("aria-expanded")).toBe("true");
    }
  );

  it("passes required to the toggle as aria-required", () => {
    const wrapper = mountListbox({ required: true });

    expect(toggle(wrapper).attributes("aria-required")).toBe("true");
  });

  it("leaves the toggle without aria-activedescendant, since focus moves to the option", async () => {
    const wrapper = mountListbox({ modelValue: OPTIONS[1] });

    expect(toggle(wrapper).attributes("aria-activedescendant")).toBeUndefined();
  });

  it.each([
    ["neutral", {}, "focus-within:border-input-focused"],
    ["valid", { valid: true }, "focus-within:outline-current"],
    ["invalid", { invalid: true }, "focus-within:outline-current"],
  ])("a %s field shows a focus indicator", (_state, props, focusClass) => {
    const wrapper = mountListbox(props);
    const field = toggle(wrapper).element.parentElement!;

    expect(field.className.split(" ")).toContain(focusClass);
  });
});

describe("Listbox, showClearButton", () => {
  const clear = (wrapper: Wrapper) => wrapper.find("#lb-listbox-clear");

  it.each([
    ["not asked for", { modelValue: OPTIONS[0] }, false],
    ["asked for but the field is empty", { showClearButton: true }, false],
    [
      "asked for and the field holds a value",
      { showClearButton: true, modelValue: OPTIONS[0] },
      true,
    ],
  ])("Clear is shown when %s", async (_title, props, shown) => {
    const wrapper = mountListbox(props);

    expect(clear(wrapper).exists()).toBe(shown);
  });

  it("Clear appears once a pick fills the field", async () => {
    const wrapper = mountListbox({ showClearButton: true });
    await open(wrapper);
    await pick(wrapper, 0);

    expect(clear(wrapper).text()).toBe("Clear");
  });

  it("Clear sets a single model to null", async () => {
    const wrapper = mountListbox({
      showClearButton: true,
      modelValue: OPTIONS[0],
    });
    await clear(wrapper).trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
  });

  it("Clear sets a multiple model to []", async () => {
    const wrapper = mountListbox({
      showClearButton: true,
      multiple: true,
      modelValue: [OPTIONS[0], OPTIONS[1]],
    });
    await clear(wrapper).trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[[]]]);
  });

  it("Clear is disabled when the field is", () => {
    const wrapper = mountListbox({
      showClearButton: true,
      disabled: true,
      modelValue: OPTIONS[0],
    });

    expect(clear(wrapper).attributes("disabled")).toBeDefined();
  });
});

describe("Listbox, multiple mode", () => {
  it("shows chips in pick order, not list order", async () => {
    const wrapper = mountListbox({ multiple: true });
    await open(wrapper);
    await pick(wrapper, 2);
    await pick(wrapper, 0);

    expect(wrapper.emitted("update:modelValue")).toEqual([
      [[OPTIONS[2]]],
      [[OPTIONS[2], OPTIONS[0]]],
    ]);
  });

  it("renders chips from the model alone, in model order", () => {
    const wrapper = mountListbox({
      multiple: true,
      modelValue: [OPTIONS[2], OPTIONS[0]],
    });

    expect(chipLabels(wrapper)).toEqual(["Group C", "Group A"]);
  });

  it("shows a chip with its label for a value that is not in the options", () => {
    const wrapper = mountListbox({
      multiple: true,
      modelValue: [{ value: "elsewhere", label: "Far away" }],
    });

    expect(chipLabels(wrapper)).toEqual(["Far away"]);
  });

  it("names each cross 'Remove <label>' and keeps the chips out of the toggle", () => {
    const wrapper = mountListbox({
      multiple: true,
      modelValue: [OPTIONS[0], OPTIONS[1]],
    });

    expect(chips(wrapper).map((chip) => chip.attributes("aria-label"))).toEqual(
      ["Remove Group A", "Remove Group B"]
    );
    expect(toggle(wrapper).findAll("button")).toHaveLength(0);
  });

  it("a cross never submits a surrounding form", async () => {
    let submitted = 0;
    const form = document.createElement("form");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      submitted++;
    });
    document.body.appendChild(form);
    const wrapper = mount(InputListbox, {
      attachTo: form,
      props: {
        id: "lb",
        options: OPTIONS,
        multiple: true,
        modelValue: [OPTIONS[0], OPTIONS[1]],
      },
    });
    await wrapper.find('button[aria-label="Remove Group A"]').trigger("click");

    expect(submitted).toBe(0);
    expect(wrapper.emitted("update:modelValue")).toEqual([[[OPTIONS[1]]]]);
  });

  it("a cross removes exactly one value", async () => {
    const wrapper = mountListbox({
      multiple: true,
      modelValue: [OPTIONS[0], OPTIONS[1], OPTIONS[2]],
    });
    await wrapper.find('button[aria-label="Remove Group B"]').trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([
      [[OPTIONS[0], OPTIONS[2]]],
    ]);
  });

  it("shows the placeholder only while no chip is present", () => {
    const empty = mountListbox({ multiple: true });
    const filled = mountListbox({ multiple: true, modelValue: [OPTIONS[0]] });

    expect(toggle(empty).text()).toBe("Select an option");
    expect(toggle(filled).text()).toBe("1 selected");
  });

  it.each([
    ["Enter", "Enter"],
    ["Space", " "],
  ])(
    "%s toggles the focused option and the panel stays open",
    async (_name, key) => {
      const wrapper = mountListbox({ multiple: true });
      await open(wrapper);
      const second = optionEls(wrapper)[1]!;

      await second.trigger("keydown", { key });
      expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual([
        [OPTIONS[1]],
      ]);
      expect(toggle(wrapper).attributes("aria-expanded")).toBe("true");

      await second.trigger("keydown", { key });
      expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual([[]]);
      expect(toggle(wrapper).attributes("aria-expanded")).toBe("true");
    }
  );

  it("Tab on an option closes a multiple panel without a pick", async () => {
    const wrapper = mountListbox({ multiple: true });
    await open(wrapper);
    await optionEls(wrapper)[1]!.trigger("keydown", { key: "Tab" });

    expect(wrapper.emitted("update:modelValue")).toBeUndefined();
    expect(toggle(wrapper).attributes("aria-expanded")).toBe("false");
  });

  it("returns focus to the toggle after a cross removes a chip", async () => {
    const wrapper = mountListbox({ multiple: true, modelValue: [OPTIONS[0]] });
    await wrapper.find('button[aria-label="Remove Group A"]').trigger("click");

    expect(document.activeElement).toBe(toggle(wrapper).element);
  });

  it("returns focus to the toggle after Clear", async () => {
    const wrapper = mountListbox({
      showClearButton: true,
      modelValue: OPTIONS[0],
    });
    await wrapper.find("#lb-listbox-clear").trigger("click");

    expect(document.activeElement).toBe(toggle(wrapper).element);
  });

  it.each([
    ["without a search box, Shift+Tab is left to the browser", false, false],
    ["with a search box, Shift+Tab moves to it", true, true],
  ])("%s", async (_title, enableSearch, prevented) => {
    const wrapper = mountListbox({ enableSearch });
    await open(wrapper);
    const event = new KeyboardEvent("keydown", {
      key: "Tab",
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    });
    optionEls(wrapper)[0]!.element.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(prevented);
  });

  it("sets aria-multiselectable on the listbox only in multiple mode", () => {
    const multiple = mountListbox({ multiple: true });
    const single = mountListbox();

    expect(
      multiple.find('ul[role="listbox"]').attributes("aria-multiselectable")
    ).toBe("true");
    expect(
      single.find('ul[role="listbox"]').attributes("aria-multiselectable")
    ).toBeUndefined();
  });

  it("marks a selected option with the checked background and a hidden checkbox icon", () => {
    const wrapper = mountListbox({
      multiple: true,
      modelValue: [OPTIONS[1]],
    });
    const [first, second] = optionEls(wrapper);

    expect(second!.attributes("aria-selected")).toBe("true");
    expect(second!.classes()).toContain("bg-input-checked");
    expect(second!.find("svg").attributes("aria-hidden")).toBe("true");
    expect(second!.find("svg").attributes("data-checked")).toBe("true");
    expect(first!.classes()).not.toContain("bg-input-checked");
  });
});

describe("Listbox, load-more", () => {
  function stubObserver() {
    const observed: Element[] = [];
    let callback: IntersectionObserverCallback = () => {};
    class MockObserver {
      constructor(cb: IntersectionObserverCallback) {
        callback = cb;
      }
      observe = (el: Element) => {
        observed.push(el);
      };
      unobserve = () => {};
      disconnect = () => {};
    }
    vi.stubGlobal("IntersectionObserver", MockObserver);
    const reachEnd = () =>
      callback(
        [{ isIntersecting: true } as IntersectionObserverEntry],
        {} as IntersectionObserver
      );
    return { observed, reachEnd };
  }

  it("emits load-more without a payload when the end of the list scrolls into view", async () => {
    const { reachEnd } = stubObserver();
    const wrapper = mountListbox();

    reachEnd();

    expect(wrapper.emitted("load-more")).toEqual([[]]);
  });

  it("observes the last child of the scroll container, again after options grow", async () => {
    const { observed } = stubObserver();
    const wrapper = mountListbox();
    const scroller = wrapper.find('ul[role="listbox"]').element.parentElement!;

    expect(observed.at(-1)).toBe(scroller.lastElementChild);

    await wrapper.setProps({
      options: [...OPTIONS, { value: "group_d", label: "Group D" }],
    });
    await flushPromises();

    expect(observed).toHaveLength(2);
    expect(observed.at(-1)).toBe(scroller.lastElementChild);
  });

  it("mounts without IntersectionObserver and emits no load-more", () => {
    vi.stubGlobal("IntersectionObserver", undefined);
    const wrapper = mountListbox();

    expect(optionEls(wrapper)).toHaveLength(3);
    expect(wrapper.emitted("load-more")).toBeUndefined();
  });
});
