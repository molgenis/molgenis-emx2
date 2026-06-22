<script setup lang="ts">
import LinkCard from "./LinkCard.vue";

interface ILinkCard {
  name: string;
  label: string;
  imageSrc?: string;
}

const props = withDefaults(
  defineProps<{
    linkCards: ILinkCard[];
    height?: "xsmall" | "small" | "medium" | "large";
  }>(),
  {
    height: "small",
  }
);
</script>

<template>
  <nav class="card-link-group">
    <ol class="d-flex flex-row m-0 p-0">
      <li class="flex-grow-1" v-for="card in linkCards">
        <LinkCard
          class="w-100"
          :imageSrc="card.imageSrc || undefined"
          :height="height"
        >
          <router-link :to="{ name: card.name }">{{ card.label }}</router-link>
        </LinkCard>
      </li>
      <slot></slot>
    </ol>
  </nav>
</template>
