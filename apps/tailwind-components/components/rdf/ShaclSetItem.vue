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

function stripUrlSchema(url: string) {
  let text = url.split("://", 2)[1];
  if(text.startsWith("www.")) text = text.substring(4);
  return text;
}
</script>

<template>
  <tr>
    <TableCell>
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
    </TableCell>
    <TableCell>
      <div class="flex flex-col gap-2.5 md:flex-row md:gap-5">
        <Button
          type="primary"
          size="tiny"
          :id="`shacl-set-${shaclSet.name}-validate`"
          :disabled="isRunning"
          @click.prevent="runShacl"
          >validate</Button
        >
        <Button
          type="outline"
          size="tiny"
          icon="plus"
          label="view"
          :disabled="isOutputDisabled"
          @click.prevent="showModal"
        />
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

        <ButtonDownloadBlob
          size="tiny"
          :disabled="isOutputDisabled"
          :data="shaclOutput"
          mediaType="text/turtle"
          :fileName="`${schema} - shacl - ${props.shaclSet.name}.ttl`"
        />
      </div>
    </TableCell>
    <TableCell>{{ shaclSet.description }}</TableCell>
    <TableCell class="text-right">{{ shaclSet.version }}</TableCell>
    <TableCell>
      <ol>
        <li v-for="source in shaclSet.sources" class="mb-2.5 last:mb-0">
          <a class="line-clamp-1" :href="source" target="_blank">{{ stripUrlSchema(source) }}</a></li
        >
      </ol>
    </TableCell>
  </tr>
</template>
