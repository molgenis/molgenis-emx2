<template>
  <VDropdown
    v-if="isSignedIn"
    aria-id="account-dropdown"
    :distance="3"
    :skidding="4"
    placement="bottom-end"
  >
    <HeaderButton label="Account" icon="user" />
    <template #popper>
      <div
        class="px-[10px] py-[5px] border-theme border-color-theme rounded-theme bg-form"
      >
        <slot v-if="hasCustomContent" />
        <template v-else>
          <section class="flex flex-col p-4">
            <div class="mb-1 text-title">Hi {{ email }}</div>

            <Button size="small" type="primary" @click="$emit('signOut')">
              Sign out
            </Button>
          </section>
        </template>
      </div>
    </template>
  </VDropdown>
  <HeaderButton
    v-else
    label="Signin"
    icon="user"
    @click="navigateTo({ path: '/login' })"
  />
</template>

<script setup lang="ts">
import { navigateTo } from "#app/composables/router";
import Button from "./Button.vue";
import HeaderButton from "./HeaderButton.vue";

defineProps<{
  hasCustomContent?: boolean;
  isSignedIn: boolean;
  email?: string;
}>();

defineEmits<{
  signOut: [];
}>();
</script>
