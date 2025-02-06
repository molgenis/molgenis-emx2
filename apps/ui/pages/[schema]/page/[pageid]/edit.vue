<template>
  <div>
    <ModularPage :content="content" :editMode="true" :page="pageid" />
  </div>
</template>

<script setup lang="ts">
import ModularPage from "~/components/ModularPage.vue";

const route = useRoute();
const schema = route.params.schema as string;
const pageid = route.params.pageid as string;
const { data: settings } = await useSettings(schema);
const content = computed(() => {
  if (settings.value) {
    const value = settings.value.filter(
      (item) => item.key === `page.${pageid}`
    )[0].value;
    try {
      return JSON.parse(value);
    } catch (e) {
      console.log(e);
      return value;
    }
  }
});
</script>
