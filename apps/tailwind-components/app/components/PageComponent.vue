<script setup lang="ts">
import type { TextElements, Headings, Img } from "../../types/pages";

import Heading from "./text/Heading.vue";
import Paragraph from "./text/Paragraph.vue";
import Image from "./Image.vue";

type ComponentTypes = Img & Headings & TextElements;

const props = withDefaults(
  defineProps<
    ComponentTypes & {
      type: string;
    }
  >(),
  {}
);
</script>

<template>
  <Heading
    v-if="['H1', 'H2', 'H3', 'H4', 'H5', 'H6'].includes(type)"
    :heading-level="headingLevel"
    :is-centered="isCentered"
  >
    {{ text }}
  </Heading>
  <Paragraph v-else-if="type === 'P'" :is-centered="isCentered">
    {{ text }}
  </Paragraph>
  <Image
    v-else-if="type === 'Img'"
    :src="src"
    :alt="alt"
    :width="width"
    :height="height"
    :is-centered="isCentered"
  />
</template>
