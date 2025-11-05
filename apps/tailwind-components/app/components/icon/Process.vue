<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import type { PropType } from 'vue'
import type {ProcessStatus} from "../../../../metadata-utils/src/generic";

defineProps({
  status: Object as PropType<ProcessStatus>
});
</script>
<template>
  <BaseIcon
      name="progress-activity"
      class="animate-spin flex-none"
      :width="30"
      v-if="status === 'RUNNING'"
  />
  <BaseIcon
      name="check"
      class="flex-none"
      :width="30"
      v-else-if="status === 'DONE'"
  />
  <BaseIcon
      name="cross"
      class="flex-none"
      :width="30"
      v-else-if="status === 'INVALID'"
  />
  <BaseIcon
      name="exclamation"
      class="flex-none"
      :width="30"
      v-else-if="status === 'ERROR'"
  />
</template>
