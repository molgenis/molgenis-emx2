<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import type { INotificationType } from "~/interfaces/types";
const props = withDefaults(
  defineProps<{
    type: INotificationType;
    subTitle?: string;
    title: string;
    timeoutInMills?: number;
  }>(),
  {
    timeoutInMills: 5000,
  }
);

let timeOutId: ReturnType<typeof setInterval>;
let show = ref(false);

function onTimeout() {
  show.value = false;
  clearInterval(timeOutId);
}

onMounted(() => {
  timeOutId = setInterval(onTimeout, props.timeoutInMills);
  show.value = true;
});

onUnmounted(() => clearInterval(timeOutId));
</script>

<template>
  <SideModal
    :show="show"
    :fullScreen="false"
    :slideInRight="true"
    buttonAlignment="right"
    :includeFooter="false"
    :type="type"
  >
    <ContentBlockModal :title="title" :subTitle="subTitle" :type="type">
      <slot></slot>
    </ContentBlockModal>
  </SideModal>
</template>
