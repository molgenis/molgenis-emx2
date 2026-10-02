<script setup lang="ts">
import { ref } from "vue";
import ColumnChart from "../../../components/viz/ColumnChart/ColumnChart.vue";
import ComponentActions from "../ComponentActions.vue";
import type { IStatisticalCharts } from "../../../../types/cms.ts";

const props = withDefaults(
  defineProps<IStatisticalCharts & { isEditable?: boolean }>(),
  {
    isEditable: false,
  }
);

const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:show="showMenu"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: 100, hide: 50 }"
    placement="bottom-start"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="ColumnCharts"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <p v-if="!chartData" class="text-title-contrast">
      No data available to build the chart. Go to
      <a href="../../StatisticalChartData" class="underline">
        Chart data table
      </a>
      to add data or edit existing records.
    </p>
    <ColumnChart v-else v-bind="props" />
  </VMenu>
  <ColumnChart v-else-if="!isEditable && chartData" v-bind="props" />
</template>
