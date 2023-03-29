<script setup>
const props = defineProps({
  image: {
    type: String,
    required: false,
  },
  link: {
    type: String,
    required: true,
  },
  linkTarget: {
    type: String,
    default: "_blank",
  },
  contact: {
    type: String,
    required: true,
  },
  contactTarget: {
    type: String,
    default: "_blank",
  },
});

let showContactInformation = ref(false);

function onContactClick() {
  window.location = props.contact;
}
</script>

<template>
  <section
    class="bg-white py-9 lg:px-12.5 px-5 text-gray-900 xl:rounded-3px shadow-primary xl:border-b-0 border-b-[1px]"
  >
    <div class="flex flex-col items-center justify-center gap-11 md:flex-row">
      <img v-if="image" class="max-h-11" :src="image" />
      <div class="flex-grow hidden align-middle md:block">
        <a
          v-if="link"
          :href="link"
          :target="linkTarget"
          class="text-blue-500 underline hover:bg-blue-50"
        >
          <BaseIcon name="external-link" class="inline mr-2" />{{ link }}
        </a>
      </div>
      <SideModal
        :show="showContactInformation"
        :fullScreen="false"
        :slideInRight="true"
        @close="showContactInformation = false"
        buttonAlignment="right"
      >
        <ContentBlock title="Contact">
          <div class="font-bold text-body-base">E-mail</div>
          <a class="text-blue-500 hover:underline" :href="`mailto:${contact}`">
            {{ contact }}
          </a>
        </ContentBlock>

        <template #footer>
          <Button
            type="secondary"
            size="small"
            label="Close"
            @click="showContactInformation = false"
            buttonAlignment="right"
          />
        </template>
      </SideModal>
      <Button
        v-if="contact"
        @click="showContactInformation = true"
        label="Contact"
        type="secondary"
        size="medium"
      />
    </div>
  </section>
</template>
