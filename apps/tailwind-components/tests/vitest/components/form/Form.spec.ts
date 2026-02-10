import { mount, flushPromises } from "@vue/test-utils";
import { expect, it, vi } from "vitest";
import Form from "../../../../app/components/form/Form.vue";
import { ref } from "vue";

let sectionsMock = ref([]);

vi.mock("../../../../app/composables/useForm", () => {
  return {
    default: vi.fn(() => ({
      requiredMessage: ref("required"),
      errorMessage: ref("error"),

      gotoPreviousRequiredField: vi.fn(),
      gotoNextRequiredField: vi.fn(),
      gotoNextError: vi.fn(),
      gotoPreviousError: vi.fn(),
      gotoSection: vi.fn(),

      insertInto: vi.fn(() => Promise.resolve()),
      updateInto: vi.fn(),

      visibleColumnErrors: ref({}),
      onUpdateColumn: vi.fn(),
      onBlurColumn: vi.fn(),
      onViewColumn: vi.fn(),

      validateAllColumns: vi.fn(),
      validateKeyColumns: vi.fn(),

      sections: sectionsMock,
      visibleColumns: ref([]),
      visibleColumnIds: ref(new Set()),
      requiredMap: ref({}),
    })),
  };
});

vi.mock("../../../../app/composables/fetchRowPrimaryKey", () => ({
  default: vi.fn(() => Promise.resolve({ id: 123 })),
}));

it("shows legend when there are multiple sections", async () => {
  sectionsMock.value = [
    {
      id: "top_of_form",
      headers: [],
      isActive: ref(false),
      label: "Top of form",
    },
    {
      id: "section_1",
      headers: ["Header 1"],
      isActive: ref(false),
      label: "Section 1",
    },
  ];
  const wrapper = mount({
    components: { Form },
    template: `
    <Suspense>
      <Form
        :metadata="metadata"
        v-model:formValues="formValues"
        :initializeAsInsert="true"
      />
    </Suspense>
  `,
    data() {
      return {
        metadata: {
          id: "my_form",
          schemaId: "schema_1",
          name: "my form",
        },
        formValues: {},
      };
    },
    global: {
      stubs: {
        FormFields: true,
        FormLegend: true,
      },
    },
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  expect(wrapper.findComponent({ name: "FormLegend" }).exists()).toBe(true);
});

it("hides legend when there is a single section without headers", async () => {
  sectionsMock.value = [{ id: "only_section", headers: [] }];
  const wrapper = mount({
    components: { Form },
    template: `
    <Suspense>
      <Form
        :metadata="metadata"
        v-model:formValues="formValues"
        :initializeAsInsert="true"
      />
    </Suspense>
  `,
    data() {
      return {
        metadata: {
          id: "my_form",
          schemaId: "schema_1",
          name: "my form",
        },
        formValues: {},
      };
    },
    global: {
      stubs: {
        FormFields: true,
        FormLegend: true,
      },
    },
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  expect(wrapper.findComponent({ name: "FormLegend" }).exists()).toBe(false);
});
