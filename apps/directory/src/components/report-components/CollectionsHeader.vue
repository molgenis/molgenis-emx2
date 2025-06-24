<template>
  Collections ({{ collectionCount }} / {{ subcollectionCount }})<sup>
    <InfoPopover popover-placement="top">
      <div>Collections: {{ collectionCount }}</div>
      <div>Subcollections: {{ subcollectionCount }}</div>
    </InfoPopover>
  </sup>
</template>

<script setup lang="ts">
//@ts-ignore
import { InfoPopover } from "molgenis-components";

const { collectionCount, subcollectionCount } = defineProps<{
  collectionCount: number;
  subcollectionCount: number;
}>();
</script>

<style scoped>
sup {
  vertical-align: super;
  font-size: small;
}
</style>
