<template>
  <ButtonBar>
    <Button
      :icon-only="true"
      icon="dark-mode"
      type="tertiary"
      label="dark mode"
      @click="setMode('dark')"
    />
    <Button
      :icon-only="true"
      icon="light-mode"
      type="tertiary"
      label="light mode"
      @click="setMode('light')"
    />
  </ButtonBar>
</template>

<script setup lang="ts">
type mode = "dark" | "light";

function setMode(mode: mode) {
  const theme = mode === "dark" ? "dark" : "light";
  document.documentElement.setAttribute("data-theme", theme);
  // todo move this to the app state ( this is just a test)
}
</script>
