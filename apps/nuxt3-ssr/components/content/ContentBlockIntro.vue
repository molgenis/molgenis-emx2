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
  contactName: {
    type: String,
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
    fieldValue: "",
    inputType: "string",
  },
  {
    name: "senderEmail",
    label: "Email",
    fieldValue: "",
    inputType: "string",
    hasError: false,
    message: "",
  },
  {
    name: "senderMessage",
    label: "Message",
    fieldValue: "",
    inputType: "textarea",
  },
]);

watch(
  () => fields[1].fieldValue,
  () => {
    fields[1].message = "";
    fields[1].hasError = false;
  }
);

const submitForm = async () => {
  const senderName = fields.find((field) => field.name === "senderName");
  const senderEmail = fields.find((field) => field.name === "senderEmail");
  const senderMessage = fields.find((field) => field.name === "senderMessage");
  // Validate form fields

  if (!senderEmail.fieldValue) {
    senderEmail.hasError = true;
    senderEmail.message = "Please enter a valid email address";
    return;
  }

  try {
    sendContactForm({
      recipientsFilter: '{"filter": {"id":{"equals":"TestCohort"}}}',
      subject: "Contact request from " + senderName.fieldValue,
      body: `Name: ${senderName.fieldValue}\nEmail: ${senderEmail.fieldValue}\nMessage: ${senderMessage.fieldValue}`,
    });
  } catch (error) {
    console.log(error);
  }

  // Reset form fields
  senderName.fieldValue = "";
  senderEmail.fieldValue = "";
  senderMessage.fieldValue = "";
  senderEmail.hasError = false;
  senderEmail.message = "";

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
        <ContentBlockModal title="Contact" :sub-title="contactName">
          <!-- <div class="font-bold text-body-base">E-mail</div>
          <a class="text-blue-500 hover:underline" :href="`mailto:${contact}`">
            {{ contact }}
          </a> -->
          <ContactForm :fields="fields" @submit-form="submitForm" />
        </ContentBlockModal>

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
