<script setup lang="ts">
import TransitionSlideUp from "../../components/transition/SlideUp.vue";
import Button from "../../components/Button.vue";
import { ref } from "vue";
const show = ref(false);
const showAutoHide = ref(false);
</script>
<template>
  <div class="flex flex-col gap-0 pb-44">
    <Button @click="show = !show">{{ show ? "Hide" : "Show" }}</Button>
    <div>container</div>
    <div
      class="border border-1 border-theme h-80 min-w-52 flex flex-col justify-end"
    >
      <TransitionSlideUp>
        <div class="bg-valid p-5 italic" v-show="show">
          “The truth is rarely pure and never simple.”
        </div>
      </TransitionSlideUp>
      <div class="z-50 bg-blue-200 h-40">other content</div>
    </div>

    <Button @click="showAutoHide = true" :disabled="showAutoHide" class="mt-5"
      >show auto hide</Button
    >
    <div
      class="border border-1 border-theme h-80 min-w-52 flex flex-col justify-end"
    >
      <TransitionSlideUp :auto-hide="true" v-model:visible="showAutoHide">
        <div class="bg-valid p-5 italic" v-show="showAutoHide">
          “I love talking about nothing. It is the only thing I know anything
          about.”
        </div>
      </TransitionSlideUp>
      <div class="z-50 bg-blue-200 h-40">other content</div>
    </div>
  </div>
</template>
