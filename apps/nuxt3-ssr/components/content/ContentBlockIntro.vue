<script setup lang="ts">
import type { INotificationType } from "~/interfaces/types";
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
    required: false,
  },
  contactName: {
    type: String,
  },
  contactTarget: {
    type: String,
    default: "_blank",
  },
  contactMessageFilter: {
    type: String,
  },
});

const useEmailService = ref(false);

fetchSetting("contactRecipientsQuery").then((resp) => {
  const setting = resp.data["_settings"].find(
    (setting: { key: string; value: string }) => {
      return setting.key === "contactRecipientsQuery";
    }
  );

  if (setting) {
    useEmailService.value = !!setting.value;
  }
});

let showContactInformation = ref(false);

const fields = reactive({
  senderName: {
    label: "Name",
    fieldValue: "",
    inputType: "string",
  },
  senderEmail: {
    label: "Email",
    fieldValue: "",
    inputType: "string",
    hasError: false,
    message: "",
  },
  senderMessage: {
    name: "senderMessage",
    label: "Message",
    fieldValue: "",
    inputType: "textarea",
  },
  topic: {
    label: "Topic",
    fieldValue: "",
    inputType: "select",
  },
});

watch(
  () => fields.senderEmail.fieldValue,
  () => {
    fields.senderEmail.message = "";
    fields.senderEmail.hasError = false;
  }
);

const showMessageStatusModal = ref(false);
const notificationType = ref("success" as INotificationType);
const notificationTitle = ref("The message has been sent");
const notificationMessage = ref("");
const timeoutInMills = ref(3000);

const submitForm = async () => {
 
  // Validate form fields

  if (!fields.senderEmail.fieldValue) {
    fields.senderEmail.hasError = true;
    fields.senderEmail.message = "Please enter a valid email address";
    return;
  }

  let isSendSuccess = false;

  try {
    isSendSuccess = await sendContactForm({
      recipientsFilter: props.contactMessageFilter || "",
      subject: "Contact request from " + fields.senderName.fieldValue,
      body: `Name: ${fields.senderName.fieldValue}\nEmail: ${fields.senderEmail.fieldValue}\nMessage: ${fields.senderMessage.fieldValue}`,
    });
  } catch (error) {
    console.log(error);
  }

  // Reset form fields
  fields.senderName.fieldValue = "";
  fields.senderEmail.fieldValue = "";
  fields.senderMessage.fieldValue = "";
  fields.senderEmail.hasError = false;
  fields.senderEmail.message = "";

  if (isSendSuccess) {
    notificationType.value = "success";
    notificationTitle.value = "The message has been sent";
    timeoutInMills.value = 3000;
  } else {
    notificationType.value = "error";
    notificationTitle.value = "Error";
    notificationMessage.value =
      "Your message could not be sent. Please try again later";
    timeoutInMills.value = 30000;
  }

  showContactInformation.value = false;
  showMessageStatusModal.value = true;
};
</script>

<template>
  <section
    class="bg-white py-9 lg:px-12.5 px-5 text-gray-900 xl:rounded-3px shadow-primary xl:border-b-0 border-b-[1px]"
  >
    <div class="flex flex-col items-center justify-center gap-11 md:flex-row">
      <img v-if="image" class="max-h-11" :src="image" />
      <div class="flex-grow hidden align-middle md:block">
        <HyperLink v-if="link" :href="link" :target="linkTarget" />
      </div>
      <SideModal
        :show="showContactInformation"
        :fullScreen="false"
        :slideInRight="true"
        @close="showContactInformation = false"
        buttonAlignment="right"
      >
        <ContentBlockModal
          title="Contact"
          :sub-title="contactName"
          class="flex flex-col gap-3"
        >
          <template v-if="contactMessageFilter && useEmailService">
            <ContactForm :fields="Object.entries(fields).map(([k, v]) => ({k, ...v}))" @submit-form="submitForm" />
            <div class="pl-3">
              <span class="text-body-base">or contact us at: </span>
              <a
                class="text-blue-500 hover:underline"
                :href="`mailto:${contact}`"
              >
                {{ contact }}
              </a>
            </div>
          </template>
          <template v-else>
            <div class="font-bold text-body-base">E-mail</div>
            <a
              class="text-blue-500 hover:underline"
              :href="`mailto:${contact}`"
            >
              {{ contact }}
            </a>
          </template>
          <ClientOnly>
            <SettingsMessage />
          </ClientOnly>
        </ContentBlockModal>

        <template #footer>
          <Button
            v-if="contactMessageFilter && useEmailService"
            type="primary"
            size="small"
            label="Send"
            @click="submitForm"
            buttonAlignment="right"
          />

          <Button
            v-else
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

    <NotificationModal
      v-if="showMessageStatusModal"
      :type="notificationType"
      :title="notificationTitle"
      :timeout-in-mills="timeoutInMills"
      :notificationMessage="notificationMessage"
      @close="showMessageStatusModal = false"
    >
      <p v-if="notificationMessage">{{ notificationMessage }}</p>
    </NotificationModal>
  </section>
</template>
