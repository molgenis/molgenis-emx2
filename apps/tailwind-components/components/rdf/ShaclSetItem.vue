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
const shaclOutput = ref<string>("");
const shaclError = ref<string>("");
const modalVisible = ref<boolean>(false);

function validateShaclOutput(output: string): boolean {
  return output
    .substring(0, 100)
    .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.");
}

async function runShacl() {
  shaclOutput.value = "";
  shaclStatus.value = "RUNNING";
  shaclError.value = "";
  modalVisible.value = false;

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
}

function showModal() {
  if (isOutputDisabled.value) return;
  modalVisible.value = !modalVisible.value;
}

const isRunning = computed(() => {
  return shaclStatus.value === "RUNNING";
});

const isOutputDisabled = computed(() => {
  return shaclStatus.value !== "VALID" && shaclStatus.value !== "INVALID";
});
</script>

<template>
  <div :id="shaclSet.name" class="justify-start items-center mb-5">
    <div class="flex justify-start items-center mb-2 space-x-1">
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
      <h3 class="uppercase text-heading-3xl font-display">
        {{ shaclSetTitle }}
      </h3>
    </div>
    <div class="flex space-x-5">
      <Button
        type="outline"
        :id="`shacl-set-${shaclSet.name}-validate`"
        :disabled="isRunning"
        @click.prevent="runShacl"
        >validate</Button
      >

      <Button
        type="outline"
        icon="plus"
        label="view"
        :disabled="isOutputDisabled"
        @click.prevent="showModal"
      />
      <ButtonDownloadBlob
        :disabled="isOutputDisabled"
        :data="shaclOutput"
        mediaType="text/turtle"
        :fileName="`${schema} - shacl - ${props.shaclSet.name}.ttl`"
      />
      <ButtonDropdown class="w-full" label="Sources">
        <div class="border-2 border-black bg-cover bg-white">
          <DisplayList class="container" type="link">
            <DisplayListItem class="truncate" v-for="source in shaclSet.sources"
              ><a :href="source" target="_blank">{{
                source
              }}</a></DisplayListItem
            >
          </DisplayList>
        </div>
      </ButtonDropdown>
    </div>
  </div>
  <Modal
    v-model:visible="modalVisible"
    :title="shaclSetTitle"
    subtitle="Validation Report"
    maxWidth="max-w-7xl"
  >
    <DisplayOutput class="px-8 my-8 min-w-full overflow-scroll">
      <pre>{{ shaclOutput }}</pre>
    </DisplayOutput>
  </Modal>
</template>
