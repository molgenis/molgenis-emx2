import { mount, flushPromises } from "@vue/test-utils";
import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { defineComponent, h, ref, Suspense } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

mockNuxtImport("useAsyncData", () => {
  return vi.fn(() => ({
    data: ref({
      data: { _session: { email: "test@test.com", admin: true } },
    }),
    error: ref(null),
    pending: ref(false),
  }));
});

import EditModal from "../../../../app/components/form/EditModal.vue";

const metadata = {
  id: "Pet",
  schemaId: "petStore",
  columns: [{ id: "name", label: "Name", columnType: "STRING", key: 1 }],
} as any;

describe("EditModal.vue", () => {
  beforeEach(() => {
    // a save that never resolves, so the test can act mid-flight
    vi.stubGlobal(
      "$fetch",
      vi.fn(() => new Promise(() => {}))
    );
  });

  function mountWithSuspense(props: Record<string, unknown>) {
    return mount(
      defineComponent({
        render() {
          return h(Suspense, null, {
            default: h(EditModal, props),
          });
        },
      }),
      {
        global: {
          stubs: {
            teleport: true,
            OptionalFocusTrap: { template: "<div><slot /></div>" },
          },
        },
      }
    );
  }

  it("disables the header close (X) button while a save is in flight", async () => {
    const wrapper = mountWithSuspense({
      schemaId: "petStore",
      metadata,
      isInsert: true,
      visible: true,
      showButton: false,
      formValues: { name: "Tweety" },
    });
    await flushPromises();

    const closeButton = () => wrapper.find('button[aria-label="Close modal"]');
    expect(closeButton().attributes("disabled")).toBeUndefined();

    const saveButton = wrapper
      .findAll("button")
      .find((b) => b.text().trim() === "Save");
    await saveButton!.trigger("click");
    await flushPromises();

    // the save is still in flight, because the mocked $fetch never resolves
    expect(closeButton().attributes("disabled")).toBeDefined();
    await closeButton().trigger("click");
    expect(wrapper.emitted("update:cancelled")).toBeUndefined();
  });
});
