<template>
  <div class="w-full box-border px-4 py-3 border rounded-md">
    <form>
      <div v-if="title" class="mb-4">
        <component
        :is="formTitleHierarchy"
        :class="`
          text-${6-parseInt(formTitleHierarchy.replace('h',''))}xl
          font-semibold
          `"
        >
          <span>{{ title }}</span>
        </component>
        <p v-if="description">{{ description }}</p>
        <div class="my-2 pb-4 border-b">
          <slot name="context"></slot>
        </div>
      </div>
      <div class="mb-4 [&>div]:mb-4">
        <slot name="inputs"></slot>
      </div>
      <div class="mb-0">
        <slot name="actions"></slot>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
interface FormProps {
  id: string,
  title?: string,
  description?: string,
  formTitleHierarchy?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6',
}

withDefaults(
  defineProps<FormProps>(),
  {
    formTitleHierarchy: 'h3'
  }
)

</script>
