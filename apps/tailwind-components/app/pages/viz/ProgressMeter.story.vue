<script setup lang="ts">
import { ref } from "vue";

import ProgressMeter from "../../components/viz/ProgressMeter/ProgressMeter.vue";
import InputNumber from "../../components/input/Int.vue";

const value = ref<number>(45);
</script>

<template>
  <div class="grid gap-2.5 grid-cols-1 md:grid-cols-3">
    <div class="col-span-2 p-5">
      <ProgressMeter
        id="progress-meter"
        title="Test"
        :value="value"
        :total="100"
        label="% complete"
      />
    </div>
    <form class="col-span-1 p-5">
      <legend>Options</legend>
      <div>
        <InputNumber id="progress-meter-value" v-model="value" />
      </div>
    </form>
  </div>
</template>
