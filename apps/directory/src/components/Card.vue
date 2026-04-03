<template>
  <article>
    <section class="d-flex flex-column align-items-center">
      <div class="d-flex flex-column h-100 align-self-stretch">
        <header class="border-0 biobank-card-header p-1">
          <slot name="header"></slot>
        </header>

        <Tabs :tabs="tabs" @update:active-tab="updateTab" />

        <slot></slot>
      </div>
    </section>
  </article>
</template>

<script setup lang="ts">
import Tabs from "./Tabs.vue";

defineProps<{
  tabs: Record<
    string,
    { id: string; label: string; active: boolean; disabled: boolean }
  >;
}>();

const emit = defineEmits(["update:activeTab"]);

function updateTab(tab: string) {
  emit("update:activeTab", tab);
}
</script>

<style>
article {
  padding: 0;
  position: relative;
  height: 28rem;
}

article {
  box-shadow: 0 6.4px 14.4px 0 rgba(0, 0, 0, 0.132),
    0 1.2px 3.6px 0 rgba(0, 0, 0, 0.108);
}

article section {
  height: 100%;
  width: 100%;
}

/* TODO put in theme */
.biobank-card-header {
  background-color: #efefef;
}
</style>
