<script setup lang="ts">
import type { ValueType } from '../../../metadata-utils/src/types';

defineProps<{
    type: ValueType;
    id: string;
}>();
</script>

<template>
    <LazyInputString v-if="type === 'STRING'" :id="id"></LazyInputString>
    <LazyInputTextArea v-else-if="type === 'TEXT'" :id="id"></LazyInputTextArea>
    <div v-else class=" border border-dotted p-2"><pre>place holder for field type {{ type }}</pre></div>
</template>