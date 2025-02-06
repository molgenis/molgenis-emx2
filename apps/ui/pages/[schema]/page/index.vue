<template>
  <div class="m-3">
    <h3>Create new page</h3>
    <div class="flex gap-1">
      <Button
        type="primary"
        @click=""
        size="small"
        label="Html Page"
        class="gap-1"
      />
      <Button type="primary" @click="" size="small" label="Text Page" />
      <Button type="primary" @click="" size="small" label="Ern page" />
      <Button type="primary" @click="" size="small" label="Ern Dashboard" />
    </div>
    <br />
    <h3>Edit page</h3>
    <div class="flex gap-1">
      <Button v-for="page in pages" type="primary" @click="" size="small" :label="page" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { InputSelect, InputString } from "molgenis-components";

const route = useRoute();
const schema = route.params.schema as string;
const { data: settings } = await useSettings(schema);
const pages = computed(() => {
  if (settings.value) {
    return settings.value
      ?.filter((item) => item.key.startsWith("page."))
      .map((item) => item.key.substring(5));
  }
});
</script>
