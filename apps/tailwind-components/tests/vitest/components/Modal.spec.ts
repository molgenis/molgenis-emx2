import { nextTick } from "vue";
import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Modal from "../../../app/components/Modal.vue";

function mountModal(visible = true) {
  let setVisible!: (v: boolean) => void;

  const wrapper = mount(Modal, {
    props: {
      visible,
      "onUpdate:visible": (v: boolean) => setVisible(v),
    },
    attachTo: document.body,
  });

  setVisible = (v: boolean) => {
    wrapper.setProps({ visible: v });
  };

  return wrapper;
}

describe("Modal stacking (Escape key)", () => {
  it("closes only the top-most modal", async () => {
    // Open first modal
    const modalA = mountModal(true);
    await nextTick();

    // Open second modal on top
    const modalB = mountModal(true);
    await nextTick();

    // Press Escape
    window.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
    await nextTick();

    // Top modal closes
    expect(modalB.props("visible")).toBe(false);

    // Bottom modal remains open
    expect(modalA.props("visible")).toBe(true);
  });
});
