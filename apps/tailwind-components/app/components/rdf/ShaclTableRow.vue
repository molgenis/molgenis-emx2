<script lang="ts" setup>
import { useRoute } from "#app/composables/router";
import { computed, ref } from "vue";
import type { ShaclSet } from "../../../../metadata-utils/src/rdf";
import BaseIcon from "../BaseIcon.vue";
import TableCell from "../TableCell.vue";
import Button from "../Button.vue";
import Modal from "../Modal.vue";
import ButtonDownloadBlob from "../button/DownloadBlob.vue";
import DisplayOutput from "../display/Output.vue";
import Message from "../Message.vue";


const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema;

const props = withDefaults(
  defineProps<{
    shaclSet: ShaclSet;
  }>(),
  {}
);

const shaclSetTitle = computed<string>(() => {
  return (
    props.shaclSet.name + " (version: " + props.shaclSet.version + ")"
  );
});

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

  const res = await fetch(`/${schema}/api/rdf?validate=${props.shaclSet.id}`);
  shaclOutput.value = await res.text();

  if (res.status !== 200) {
    shaclStatus.value = "ERROR";
    shaclError.value = `Error (status code: ${res.status})`;
  } else if (validateShaclOutput(shaclOutput.value)) {
    shaclStatus.value = "VALID";
  } else {
    shaclStatus.value = "INVALID";
  }
}

function showModal() {
  if (isViewDisabled.value) return;
  modalVisible.value = !modalVisible.value;
}

const isRunning = computed(() => {
  return shaclStatus.value === "RUNNING";
});

const isViewDisabled = computed(() => {
  return shaclStatus.value === "RUNNING" || shaclStatus.value === "UNKNOWN";
});

const isDownloadDisabled = computed(() => {
  return shaclStatus.value !== "VALID" && shaclStatus.value !== "INVALID";
});
</script>

<template>
  <tr>
    <TableCell>
      <BaseIcon
        name="progress-activity"
        class="animate-spin m-auto"
        width.number="32"
        v-if="shaclStatus === 'RUNNING'"
      />
      <BaseIcon
        name="check"
        class="m-auto"
        width.number="32"
        v-else-if="shaclStatus === 'VALID'"
      />
      <BaseIcon
        name="cross"
        class="m-auto"
        width.number="32"
        v-else-if="shaclStatus === 'INVALID'"
      />
      <BaseIcon
        name="exclamation"
        class="m-auto"
        width.number="32"
        v-else-if="shaclStatus === 'ERROR'"
      />
    </TableCell>
    <TableCell>
      <div class="flex flex-col gap-2.5 md:flex-row md:gap-5">
        <Button
          type="primary"
          size="tiny"
          :id="`shacl-set-${shaclSet.id}-validate`"
          :disabled="isRunning"
          @click.prevent="runShacl"
          >validate</Button
        >
        <Button
          type="outline"
          size="tiny"
          icon="plus"
          label="view"
          :disabled="isViewDisabled"
          @click.prevent="showModal"
        />
        <Modal
          v-model:visible="modalVisible"
          :title="shaclSetTitle"
          subtitle="Validation Report"
          maxWidth="max-w-7xl"
        >
          <Message
            id="`shacl-run-${props.shaclSet.id}-error`}`"
            class="my-2"
            :invalid="true"
            v-if="shaclError"
          >
            <span>{{ shaclError }}</span>
          </Message>
          <DisplayOutput class="px-8 my-8 min-w-full overflow-scroll">
            <pre>{{ shaclOutput }}</pre>
          </DisplayOutput>
        </Modal>

        <ButtonDownloadBlob
          size="tiny"
          :disabled="isDownloadDisabled"
          :data="shaclOutput"
          mediaType="text/turtle"
          :fileName="`${schema} - shacl - ${props.shaclSet.id}.ttl`"
        />
      </div>
    </TableCell>
    <TableCell>{{ shaclSet.name }}</TableCell>
    <TableCell class="text-right">{{ shaclSet.version }}</TableCell>
    <TableCell>
      <ol>
        <li v-for="source in shaclSet.sources" class="mb-2.5 last:mb-0">
          <a class="line-clamp-1" :href="source" target="_blank">{{
            source
          }}</a>
        </li>
      </ol>
    </TableCell>
  </tr>
</template>
