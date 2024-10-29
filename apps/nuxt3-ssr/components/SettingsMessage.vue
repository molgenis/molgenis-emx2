<script setup lang="ts">
const CATALOGUE_LANDING_TITLE = "CATALOGUE_BCC_MSG";
const message = ref();

fetchSetting(CATALOGUE_LANDING_TITLE).then((resp) => {
  const setting = resp.data["_settings"].find(
    (setting: { key: string; value: string }) => {
      return setting.key === CATALOGUE_LANDING_TITLE;
    }
  );

  if (setting) {
    message.value = setting.value;
  }
});
</script>
<template>
  <div v-if="message" class="pl-3">
    <p class="text-body-base">{{ message }}</p>
  </div>
</template>
