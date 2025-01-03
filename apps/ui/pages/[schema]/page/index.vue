<template>
  <div>
    <p>Create new page</p>
    <InputString></InputString>
    <InputSelect
      required
      :options="['html', 'text page', 'ern page', 'dashboard']"
    />

    <br />
    <p>view all pages</p>
    {{ pages }}
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
