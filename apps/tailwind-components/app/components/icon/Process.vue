<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import type {ProcessStatus} from "../../../../metadata-utils/src/generic";
import type {AsyncDataRequestStatus} from "#app";

defineProps<{
  status: ProcessStatus | AsyncDataRequestStatus;
  width?: number;
}>()
</script>
<template>
  <BaseIcon
      name="progress-activity"
      class="animate-spin flex-none"
      :width="width"
      v-if="status === 'RUNNING' || status === 'pending'"
  />
  <BaseIcon
      name="check"
      class="flex-none"
      :width="width"
      v-else-if="status === 'DONE' || status === 'success'"
  />
  <BaseIcon
      name="cross"
      class="flex-none"
      :width="width"
      v-else-if="status === 'INVALID'"
  />
  <BaseIcon
      name="exclamation"
      class="flex-none"
      :width="width"
      v-else-if="status === 'ERROR' || status === 'error'"
  />
</template>
