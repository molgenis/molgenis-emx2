<script setup lang="ts">
import type {
  IOntlogyColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import { getLabel } from "../../utils/refUtils";
const props = defineProps<{
  metadata: IOntlogyColumn;
  data: IRow;
}>();

const label = getLabel(props.metadata, props.data);
</script>

<template>
  <span class="underline hover:cursor-pointer text-link">{{ label }}</span>
</template>
