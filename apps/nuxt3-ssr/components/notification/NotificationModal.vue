<script setup lang="ts">
import { INotificationType } from "~/interfaces/types";
const { type, subTitle, title, timeoutInMills } = withDefaults(
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

let timeOutId: NodeJS.Timer;
let show = ref(false);

function onTimeout() {
  show.value = false;
  clearInterval(timeOutId);
}

onMounted(() => {
  timeOutId = setInterval(onTimeout, timeoutInMills);
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
      <slot>{{ message }}</slot>
    </ContentBlockModal>
  </SideModal>
</template>
