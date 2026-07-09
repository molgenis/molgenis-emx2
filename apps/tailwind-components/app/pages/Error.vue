<template>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative">
      <main>
        <slot>
          <div class="flex h-screen">
            <div class="m-auto w-1/3">
              <div class="items-center justify-between hidden xl:flex h-25">
                <Logo />
              </div>
              <div class="font-display text-heading-7xl">
                Oops! something went wrong.
              </div>
              <div v-if="error?.message" class="py-5 text-heading-4xl">
                <p>{{ error?.message }}</p>
              </div>
              <div class="py-5 text-xl">Status code: {{ error?.status }}</div>

              <div class="py-5">
                <Button @click="handleError" label="Home" size="medium" />
              </div>
            </div>
          </div>
        </slot>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Button from "../components/Button.vue";
import { clearError, type NuxtError } from "nuxt/app";

const props = defineProps<{ error: NuxtError }>();

const handleError = () => clearError({ redirect: "/" });
</script>
