<template>
  <h1>Figma</h1>
  <DesignColors class="w-[100%]" />
</template>

<script lang="ts" setup>
import { onMounted } from "vue";
const loading = ref<boolean>(true);

onMounted(async () => {
  const file = "~/assets/files/molgenis_20221006_export.fig";
});
</script>
