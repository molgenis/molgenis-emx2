import { mount, flushPromises } from "@vue/test-utils";
import { describe, it, expect, vi } from "vitest";
import { defineComponent, h, Suspense } from "vue";
import Form from "../../../../app/components/form/Form.vue";

vi.mock("../Fields.vue", () => ({
  default: defineComponent({
    name: "FormFields",
    template: "<div />",
  }),
}));

describe("Form.vue", () => {
  function createFormMock() {
    return {
      resetRowKey: vi.fn().mockResolvedValue(undefined),
      visibleColumns: vi.fn().mockResolvedValue([]),
      scrollContainerId: { value: "mocked-id" },
    };
  }

  // Helper function to mount the Form component with Suspense to handle async operations
  function mountWithSuspense(props: any) {
    return mount(
      defineComponent({
        render() {
          return h(Suspense, null, {
            default: h(Form, props),
          });
        },
      })
    );
  }

  it("calls resetRowKey when initializeAsInsert is false", async () => {
    const formMock = createFormMock();

    mountWithSuspense({
      form: formMock,
      initializeAsInsert: false,
    });

    await flushPromises();

    expect(formMock.resetRowKey).toHaveBeenCalledTimes(1);
  });

  it("does not call resetRowKey when initializeAsInsert is true", async () => {
    const formMock = createFormMock();

    mountWithSuspense({
      form: formMock,
      initializeAsInsert: true,
    });

    await flushPromises();

    expect(formMock.resetRowKey).not.toHaveBeenCalled();
  });
});
