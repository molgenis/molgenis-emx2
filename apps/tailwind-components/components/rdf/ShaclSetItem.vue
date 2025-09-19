<script lang="ts" setup>
import { useRoute } from "#app/composables/router";
import { computed, ref } from "vue";
import { useFetch } from "#app";
import type { ShaclSetItem } from "../../../metadata-utils/src/rdf";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema;

const props = withDefaults(
  defineProps<{
    shaclSet: ShaclSetItem;
  }>(),
  {}
);

const shaclSetTitle = computed<string>(() => {
  return (
    props.shaclSet.description + " (version: " + props.shaclSet.version + ")"
  );
});

type Resp<T> = {
  data: Record<string, T>;
};

type ShaclStatus = "UNKNOWN" | "RUNNING" | "VALID" | "INVALID" | "ERROR";

const shaclStatus = ref<ShaclStatus>("UNKNOWN");
const shaclOutput = ref<string>("Validate schema to view output");
const shaclError = ref<string>("");
const isExpanded = ref<boolean>(false);
const isDisabled = ref<boolean>(false);
const showModal = ref<boolean>(false);

function validateShaclOutput(output: string): boolean {
  return  output.substring(0, 100).includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.");
}

async function runShacl() {
  isDisabled.value = true;
  shaclOutput.value = "Running validation. Please wait.";
  shaclStatus.value = "RUNNING";
  shaclError.value = "";

  const { data, error, status } = await useFetch<Resp<string>>(
    `/${schema}/api/rdf?validate=${props.shaclSet.name}`
  );

  shaclOutput.value = data.value as unknown as string;

  if (!data.value || error.value || status.value === "error") {
    shaclError.value = `ERROR: ${error.value}`;
    shaclStatus.value = "ERROR";
  } else if (validateShaclOutput(shaclOutput.value)) {
    shaclStatus.value = "VALID";
  } else {
    shaclStatus.value = "INVALID";
  }

  isDisabled.value = false;
}
</script>

<template>
  <div :id="shaclSet.name" class="border-t border-input">
    <div class="flex justify-start items-center gap-2">
      <button
        :id="`shacl-set-${shaclSet.name}-toggle`"
        :aria-controls="`shacl-set-${shaclSet.name}-content`"
        :aria-expanded="isExpanded"
        @click="isExpanded = !isExpanded"
        class="py-5 pl-2 w-full flex justify-start items-center gap-2"
      >
        <BaseIcon
          name="caret-down"
          class="origin-center transition-all duration-default"
          :class="{
            'rotate-0': !isExpanded,
            'rotate-180': isExpanded,
          }"
        />
        <span>{{ shaclSetTitle }}</span>
      </button>
      <div>
        <BaseIcon
          name="progress-activity"
          class="animate-spin"
          v-if="shaclStatus === 'RUNNING'"
        />
        <BaseIcon name="check" v-else-if="shaclStatus === 'VALID'" />
        <BaseIcon name="cross" v-else-if="shaclStatus === 'INVALID'" />
        <BaseIcon name="exclamation" v-else-if="shaclStatus === 'ERROR'" />
      </div>
      <Button
        type="outline"
        size="tiny"
        :id="`shacl-set-${shaclSet.name}-validate`"
        class="mr-2"
        :disabled="isDisabled"
        @click.prevent="runShacl"
      >
        validate
      </Button>
    </div>
    <div
      class="py-2 pb-6"
      :class="{
        hidden: !isExpanded,
      }"
    >
      <div class="p-2 flex justify-start items-center">
        <p class="w-full">Validation Report</p>
        <Button
          type="outline"
          :icon-only="true"
          icon="plus"
          size="tiny"
          label="View in full screen"
          @click="showModal = !showModal"
        />
      </div>
      <Message
        :id="`shacl-set-${shaclSet.name}-output-error-message`"
        :invalid="true"
        class="my-2"
        v-if="shaclError"
      >
        <span>{{ shaclError }}</span>
      </Message>
      <DisplayOutput class="max-h-60 overflow-x-hidden">
        <pre>{{ shaclOutput }}</pre>
      </DisplayOutput>
    </div>
  </div>
  <Modal
    v-model:visible="showModal"
    :title="shaclSetTitle"
    subtitle="Validation Report"
    maxWidth="max-w-7xl"
  >
    <DisplayOutput class="px-8 my-8">
      <pre>{{ shaclOutput }}</pre>
    </DisplayOutput>
  </Modal>
</template>
