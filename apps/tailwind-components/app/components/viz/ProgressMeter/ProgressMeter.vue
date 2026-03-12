<script lang="ts">
import { computed, useTemplateRef } from "vue";

import ChartTitle from '../ChartTitle.vue';

import type { ProgressMeter } from '../../../../types/viz';

const props = withDefaults(
    defineProps<ProgressMeter>(),
        {
            color: "",
        }
)

const container = useTemplateRef("container");
const parentElem = computed<HTMLElement>(() => {
  return container.value?.parentNode as HTMLElement;
});


</script>

<template>
    <div ref="container" class="grid gap-2.5 w-ful chart_layout_default">
        <ChartTitle
            :title="title"
            :description="description"
            style="grid-area: context;"
        />
    </div>
</template>