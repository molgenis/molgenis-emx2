<template>
  <div class="mb-3" :class="indentationLevel">
    <b>
      <router-link :to="'/collection/' + collection.id">
        {{ collection.name }}
      </router-link>
    </b>
    <view-generator :viewmodel="collection.viewmodel" />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import ViewGenerator from "../ViewGenerator.vue";

const { collection, level } = withDefaults(
  defineProps<{ collection: any; level?: number }>(),
  { level: 1 }
);

const indentationLevel = computed(() => `ml-${level}`);
</script>
