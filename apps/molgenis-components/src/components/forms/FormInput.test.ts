import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import FormInput from "./FormInput.vue";
import InputRefBack from "./InputRefBack.vue";

// shallow: this test asserts only which input FormInput dispatches to, and FormInput itself
// touches no client. Mounting for real also mounts InputRefBack, which reaches the network via
// `resolveTablePermission` — a named export a module-level client mock does not intercept,
// because InputRefBack imports "../../client/client.ts" while the mock names
// "../../client/client". Nothing awaits that call, so each mount leaked an unhandled rejection
// and failed the run even though the assertions passed.
async function mountFormInput(columnType: string) {
  const wrapper = mount(FormInput, {
    shallow: true,
    props: {
      id: "kennels",
      label: "Kennels",
      columnType,
      schemaId: "pet store",
      tableId: "Kennel",
      refBackId: "shelter",
      refLabel: "${name}",
    },
  });
  await flushPromises();
  return wrapper;
}

describe("FormInput type dispatch", () => {
  test("it should render the refback input for a PARTS column, like it does for REFBACK", async () => {
    const refBackWrapper = await mountFormInput("REFBACK");
    const partsWrapper = await mountFormInput("PARTS");

    expect(refBackWrapper.findComponent(InputRefBack).exists()).toBe(true);
    expect(partsWrapper.findComponent(InputRefBack).exists()).toBe(true);
    expect(partsWrapper.text()).not.toContain("UNSUPPORTED TYPE");
  });
});
