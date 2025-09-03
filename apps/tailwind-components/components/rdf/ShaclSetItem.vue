<script lang="ts" setup>
import { useRoute } from "#app/composables/router";
import { ref, computed } from "vue";
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

enum ShaclStatus {
  UNKNOWN,
  VALID = "check",
  INVALID = "exclamation",
  RUNNING = "progress-activity",
  ERROR = "exclamation",
}

const isExpanded = ref<boolean>(false);
const isDisabled = ref<boolean>(false);
const shaclOutput = ref<string>("");
const shaclStatus = ref<ShaclStatus>(ShaclStatus.UNKNOWN);
const error = ref<string>("");

const shaclSetTitle = computed<string>(() => {
  return (
    props.shaclSet.description + " (version: " + props.shaclSet.version + ")"
  );
});

async function runShacl() {
  isDisabled.value = true;
  error.value = "";
  shaclOutput.value = "";
  shaclStatus.value = ShaclStatus.RUNNING;

  const res = await fetch(`/${schema}/api/rdf?validate=${props.shaclSet.name}`);
  shaclOutput.value = await res.text();
  if (res.status !== 200) {
    shaclStatus.value = ShaclStatus.ERROR;
    error.value = "Error (status code: " + res.status + ")";
  } else if (
    shaclOutput.value
      .substring(0, 100)
      .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.")
  ) {
    shaclStatus.value = ShaclStatus.VALID;
  } else {
    shaclStatus.value = ShaclStatus.INVALID;
  }
  isDisabled.value = false;
}
</script>

<template>
  <div :id="shaclSet.name" class="border-b border-b-input">
    <div class="flex justify-start items-center">
      <button
        :id="`shacl-set-${shaclSet.name}-toggle`"
        :aria-controls="`shacl-set-${shaclSet.name}-content`"
        :aria-expanded="isExpanded"
        @click="isExpanded = !isExpanded"
        class="py-5 pl-2 w-full flex justify-start items-center"
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
        <BaseIcon :name="shaclStatus" />
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
      class="p-2"
      :class="{
        hidden: !isExpanded,
      }"
    >
      <p>{{ shaclSet.description }}</p>
    </div>
  </div>
</template>
