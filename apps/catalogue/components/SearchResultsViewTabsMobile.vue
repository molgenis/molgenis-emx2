<script setup lang="ts">
import { computed } from "vue";

const props = defineProps({
  buttonTopLabel: {
    type: String,
  },
  buttonTopName: {
    type: String,
  },
  buttonTopIcon: {
    type: String,
  },
  buttonBottomLabel: {
    type: String,
  },
  buttonBottomName: {
    type: String,
  },
  buttonBottomIcon: {
    type: String,
  },
  activeName: {
    type: String,
  },
});

const emit = defineEmits(["update:activeName"]);

const label = computed(() => {
  return props.activeName === props.buttonTopName
    ? props.buttonTopLabel
    : props.buttonBottomLabel;
});

const icon = computed(() => {
  return props.activeName === props.buttonTopName
    ? props.buttonTopIcon
    : props.buttonBottomIcon;
});

function handleClick() {
  emit(
    "update:activeName",
    props.activeName === props.buttonTopName
      ? props.buttonBottomName
      : props.buttonTopName
  );
}
</script>
<template>
  <div class="flex justify-between">
    <SideModal :fullScreen="false">
      <template #button>
        <Button
          type="primary"
          size="small"
          label="filters"
          icon="filter"
          iconPosition="left"
        ></Button>
      </template>

      <ContentBlockModal title="Filters">
        <slot></slot>
      </ContentBlockModal>

      <template #footer="{ hide }">
        <Button
          type="secondary"
          size="small"
          label="View results"
          iconPosition="left"
          @click="hide()"
        ></Button>
      </template>
    </SideModal>

    <Button
      type="secondary"
      size="small"
      :label="label"
      :icon="icon"
      @click="handleClick"
    />
  </div>
</template>
