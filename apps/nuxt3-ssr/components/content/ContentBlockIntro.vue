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

const fields = reactive([
  {
    name: "senderName",
    label: "Name",
    value: "",
    inputType: "string",
  },
  {
    name: "senderEmail",
    label: "Email",
    value: "",
    inputType: "string",
  },
  {
    name: "senderMessage",
    label: "Message",
    value: "",
    inputType: "textarea",
  },
]);

const submitForm = async () => {
  const senderName = fields.find((field) => field.name === "senderName");
  const senderEmail = fields.find((field) => field.name === "senderEmail");
  const senderMessage = fields.find((field) => field.name === "senderMessage");
  // Validate form fields
  if (!senderName.value || !senderEmail.value || !senderMessage.value) {
    alert("Please fill in all fields");
    return;
  }

  await sendContactForm({
    recipientsFilter: '{"filter": {"id":{"equals":"TestCohort"}}}',
    subject: "Contact request from " + senderName.value,
    body: `Name: ${senderName.value}\nEmail: ${senderEmail.value}\nMessage: ${senderMessage.value}`,
  });

  // Reset form fields
  senderName.value = "";
  senderEmail.value = "";
  senderMessage.value = "";

  alert("Form submitted successfully");

  showContactInformation.value = false;
};
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
          <!-- <div class="font-bold text-body-base">E-mail</div>
          <a class="text-blue-500 hover:underline" :href="`mailto:${contact}`">
            {{ contact }}
          </a> -->
          <ContactForm :fields="fields" @submit-form="submitForm" />
        </ContentBlock>

        <template #footer>
          <Button
            type="primary"
            size="small"
            label="Send"
            @click="submitForm"
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
