<script setup lang="ts">
import type {
  IFormField,
  INotificationType,
  linkTarget,
} from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    image?: string;
    link?: string;
    linkTarget?: linkTarget;
    contact?: string;
    contactName?: string;
    contactTarget?: string;
    contactMessageFilter?: string;
    subjectTemplate?: string;
  }>(),
  {
    linkTarget: "_blank",
    contactTarget: "_blank",
  }
);

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
    name: "senderName",
    label: "Name",
    fieldValue: "",
    inputType: "string",
  },
  senderEmail: {
    name: "senderEmail",
    label: "Email",
    fieldValue: "",
    inputType: "string",
    hasError: false,
    message: "",
  },
  organization: {
    name: "organization",
    label: "Organisation",
    fieldValue: "",
    inputType: "string",
  },
  topic: {
    name: "topic",
    label: "Topic",
    fieldValue: "",
    placeholder: "Please select a topic",
    inputType: "select",
    options: [
      "Data / sample request",
      "Collaboration request",
      "Information request",
      "Other",
    ],
  },
  senderMessage: {
    name: "senderMessage",
    label: "Message",
    fieldValue: "",
    inputType: "textarea",
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

  const subject = props.subjectTemplate
    ? props.subjectTemplate + ` ${fields.topic.fieldValue}`
    : `Contact request for ${fields.senderName.fieldValue}`;

  try {
    isSendSuccess = await sendContactForm({
      recipientsFilter: props.contactMessageFilter || "",
      subject,
      body: `
        Name: ${fields.senderName.fieldValue}
      \nEmail: ${fields.senderEmail.fieldValue}
      \nOrganization: ${fields.organization.fieldValue}
      \nTopic: ${fields.topic.fieldValue}
      \nMessage: ${fields.senderMessage.fieldValue}
    `,
    });
  } catch (error) {
    console.log(error);
  }

  // Reset form fields
  fields.senderName.fieldValue = "";
  fields.senderEmail.fieldValue = "";
  fields.organization.fieldValue = "";
  fields.topic.fieldValue = "";
  fields.senderEmail.message = "";
  fields.senderMessage.fieldValue = "";
  fields.senderEmail.hasError = false;

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
            <ContactForm
              :fields="fields as Record<string, IFormField>"
              @submit-form="submitForm"
            />
            <hr class="border-gray-300 my-4" />
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
