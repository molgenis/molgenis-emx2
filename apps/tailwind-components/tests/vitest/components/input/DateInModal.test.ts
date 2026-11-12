import { defineComponent, h, nextTick, type Component } from "vue";
import { describe, it, expect, afterEach } from "vitest";
import { mount, flushPromises, type VueWrapper } from "@vue/test-utils";
import Modal from "../../../../app/components/Modal.vue";
import InputDate from "../../../../app/components/input/Date.vue";
import InputDateTime from "../../../../app/components/input/DateTime.vue";

// The modal's focus trap blocks clicks outside the dialog. The date picker
// popup is teleported, so it must land inside the modal to stay clickable.

let wrapper: VueWrapper | undefined;

afterEach(() => {
  wrapper?.unmount();
  wrapper = undefined;
});

async function settle() {
  for (let i = 0; i < 5; i++) {
    await flushPromises();
    await nextTick();
    await new Promise((resolve) => setTimeout(resolve, 20));
  }
}

function click(element: HTMLElement) {
  element.dispatchEvent(
    new MouseEvent("mousedown", { bubbles: true, cancelable: true })
  );
  element.dispatchEvent(
    new MouseEvent("mouseup", { bubbles: true, cancelable: true })
  );
  element.click();
}

describe.each([
  ["Date", InputDate, "2024-03-01", "2024-03-15"],
  ["DateTime", InputDateTime, "2024-03-01T10:30:00", "2024-03-15 10:30:00"],
] as [string, Component, string, string][])(
  "%s input inside a Modal",
  (_name, input, initialValue, expectedValue) => {
    it("selects a day clicked in the picker popup", async () => {
      const emitted: string[] = [];
      wrapper = mount(
        defineComponent({
          setup() {
            return () =>
              h(Modal, { visible: true, title: "modal" }, () =>
                h(input, {
                  id: "date-in-modal",
                  modelValue: initialValue,
                  "onUpdate:modelValue": (value: string) => emitted.push(value),
                })
              );
          },
        }),
        { attachTo: document.body }
      );
      await settle();

      const dateInput = document.querySelector<HTMLInputElement>(
        "#dp-input-date-in-modal"
      )!;
      click(dateInput);
      dateInput.focus();
      await settle();

      const day = Array.from(
        document.querySelectorAll<HTMLElement>(".dp__cell_inner")
      ).find((cell) => cell.textContent?.trim() === "15");
      expect(day).toBeDefined();
      click(day!);
      await settle();

      expect(emitted.at(-1)).toBe(expectedValue);
    });
  }
);
