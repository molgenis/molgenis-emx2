<script setup lang="ts">
import { useId } from "vue";
import type {
  columnId,
  columnValue,
} from "../../../../metadata-utils/src/types";
import { type UseForm } from "../../composables/useForm";
import FormFields from "./Fields.vue";

const props = defineProps<{
  form: UseForm;
  constantValues?: Record<columnId, columnValue>;
  initializeAsInsert?: boolean;
}>();

if (!props.initializeAsInsert) {
  await props.form.resetRowKey();
}

// make sure unique scroll container id is generated and shared via useFrom
props.form.setScrollContainerId(useId());
</script>
<template>
  <div :id="form.getScrollContainerId().value" class="overflow-y-auto p-12.5">
    <FormFields :form="form" :constantValues="constantValues" />
  </div>
</template>
