<template>
  <nav class="quick-links">
    <ol class="quick-links-nav">
      <li class="nav-item" v-for="row in data">
        <LinkCard :imageSrc="imageSrc ? row[imageSrc] : null" :height="height">
          <router-link :to="{ name: row[name] }">{{ row[label] }}</router-link>
        </LinkCard>
      </li>
      <!-- Slot for displaying additional links -->
      <slot></slot>
    </ol>
  </nav>
</template>

<script setup lang="ts">
// @ts-ignore
import { LinkCard } from "molgenis-viz";

interface IObjectKeyPairs {
  [key: string]: string;
}

withDefaults(
  defineProps<{
    data: IObjectKeyPairs[];
    name: string;
    label: string;
    imageSrc: string;
    height?: "xsmall" | "small" | "medium" | "large";
  }>(),
  {
    height: "small",
  }
);
</script>

<style lang="css">
.quick-links .quick-links-nav {
  display: flex;
  flex-direction: row;
  list-style: none;
  padding: 0;
  margin: 0;
}

.quick-links .quick-links-nav li {
  flex-grow: 1;
}

.quick-links .quick-links-nav li .link-card {
  width: 100%;
}
</style>
