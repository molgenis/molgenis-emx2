<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "./BaseIcon.vue";
import type { MenuItem } from "../../types/types";
import { useAppConfig } from "#app";

const appConfig = useAppConfig();
const basePath = appConfig.basePath || "/";

const props = withDefaults(
  defineProps<{
    navigation: MenuItem[];
    showMoreButton?: boolean;
    invert?: boolean;
    maximumButtonShown?: number;
  }>(),
  {
    showMoreButton: true,
    invert: false,
    maximumButtonShown: 4,
  }
);

const mainNavItems = computed(() =>
  props.navigation.slice(0, props.maximumButtonShown)
);
const overFlowItems = computed(() =>
  props.navigation.slice(props.maximumButtonShown)
);
</script>

<template>
  <nav class="flex items-center justify-between gap-6 xl:justify-center">
    <template v-for="mainNavItem in mainNavItems">
      <template v-if="!mainNavItem.submenu">
        <NuxtLink
          v-if="mainNavItem.isSpaLink"
          :to="mainNavItem.link"
          class="flex items-center gap-1 tracking-widest transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline whitespace-nowrap"
          :class="invert ? 'text-sub-menu' : 'text-menu'"
        >
          {{ mainNavItem.label }}
        </NuxtLink>
        <a
          v-else
          :href="basePath + mainNavItem.link"
          class="flex items-center gap-1 tracking-widest transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline whitespace-nowrap"
          :class="invert ? 'text-sub-menu' : 'text-menu'"
        >
          {{ mainNavItem.label }}
        </a>
      </template>
      <template v-else>
        <VMenu placement="bottom-end" :distance="-1">
          <button
            class="flex items-center gap-1 tracking-widest transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline whitespace-nowrap"
            :class="invert ? 'text-sub-menu' : 'text-menu'"
          >
            {{ mainNavItem.label }}
            <BaseIcon name="caret-down" />
          </button>

          <template #popper>
            <ol
              class="flex flex-col gap-1.5 text-body-base rounded-3px rounded-tr-none shadow-xl p-6 bg-form"
            >
              <li v-for="subNavItem in mainNavItem.submenu">
                <template v-if="!subNavItem.submenu">
                  <NuxtLink
                    v-if="subNavItem.isSpaLink"
                    :to="subNavItem.link"
                    class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline whitespace-nowrap"
                  >
                    {{ subNavItem.label }}
                  </NuxtLink>
                  <a
                    v-else
                    :href="basePath + subNavItem.link"
                    class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline whitespace-nowrap"
                  >
                    {{ subNavItem.label }}
                  </a>
                </template>
                <template v-else>
                  <VMenu placement="right-start" :distance="-1">
                    <button
                      class="flex items-center gap-1 tracking-widest transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline whitespace-nowrap"
                      :class="invert ? 'text-sub-menu' : 'text-menu'"
                    >
                      {{ subNavItem.label }}
                      <BaseIcon name="caret-right" />
                    </button>

                    <template #popper>
                      <ol
                        class="flex flex-col gap-1.5 text-body-base rounded-3px rounded-tr-none shadow-xl p-6 bg-form"
                      >
                        <li v-for="subSubNavItem in subNavItem.submenu">
                          <NuxtLink
                            v-if="subSubNavItem.isSpaLink"
                            :to="subSubNavItem.link"
                            class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline whitespace-nowrap"
                          >
                            {{ subSubNavItem.label }}
                          </NuxtLink>
                          <a
                            v-else
                            :href="basePath + subSubNavItem.link"
                            class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline whitespace-nowrap"
                          >
                            {{ subSubNavItem.label }}
                          </a>
                        </li>
                      </ol>
                    </template>
                  </VMenu>
                </template>
              </li>
            </ol>
          </template>
        </VMenu>
      </template>
    </template>

    <VMenu
      placement="bottom-end"
      :distance="-1"
      v-if="overFlowItems.length > 0 && showMoreButton"
    >
      <button
        class="flex items-center gap-1 pt-3 pb-2 pl-4 pr-2 -mt-3 -ml-4 tracking-widest transition-colors duration-300 translate-y-1 border border-b-0 border-transparent rounded-t-3px font-display text-heading-xl hover:border-white"
        :class="invert ? 'text-sub-menu' : 'text-menu'"
      >
        More
        <BaseIcon name="caret-down" />
      </button>

      <template #popper>
        <ol
          class="flex flex-col gap-1.5 text-body-base rounded-3px rounded-tr-none shadow-xl p-6 bg-form"
        >
          <li v-for="overFlowItem in overFlowItems">
            <NuxtLink
              v-if="overFlowItem.isSpaLink"
              :to="overFlowItem.link"
              class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline whitespace-nowrap"
            >
              {{ overFlowItem.label }}
            </NuxtLink>
            <a
              v-else
              :href="basePath + overFlowItem.link"
              class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline whitespace-nowrap"
            >
              {{ overFlowItem.label }}
            </a>
          </li>
        </ol>
      </template>
    </VMenu>
  </nav>
</template>

<style>
.v-popper--theme-menu .v-popper__arrow-container {
  display: none;
}

.v-popper--theme-menu .v-popper__inner {
  border-radius: 0;
  box-shadow: none;
  border: 0;
  background: none;
  padding: 0;
  overflow: unset;
}
</style>
