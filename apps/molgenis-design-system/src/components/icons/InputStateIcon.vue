<template>
  <CheckCircleIcon class="w-[1.2rem] h-[1.2rem]" v-if="valid" />
  <ExclamationTriangleIcon class="w-[1.2rem] h-[1.2rem]" v-else-if="error" />
  <NoSymbolIcon class="w-[1.2rem] h-[1.2rem]" v-else-if="disabled" />
</template>

<script setup lang="ts">
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  NoSymbolIcon
} from "@heroicons/vue/24/solid";

interface Props {
  error?: boolean,
  disabled?: boolean,
  valid?: boolean,
}

defineProps<Props>()
</script>