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
// @ts-expect-error
import { LinkCard } from "molgenis-viz";

withDefaults(
  defineProps<{
    data: Record<string, any>;
    name: string;
    label: string;
    imageSrc: string;
    height?: "xsmall" | "small" | "medium" | "large";
  }>(),
  {
    height: "medium",
  }
);
</script>

<style lang="scss">
.quick-links {
  .quick-links-nav {
    display: flex;
    flex-direction: row;
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      flex-grow: 1;
    }

    .link-card {
      width: 100%;
      border-radius: 0;

      .card-background-filter {
        border-radius: 0;
      }
    }
  }
}
</style>
