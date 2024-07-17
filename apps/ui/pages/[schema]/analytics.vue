<script setup lang="ts">
definePageMeta({
  middleware: "admin-only",
});
const route = useRoute();
const schema = route.params.schema as string;
interface Trigger {
  name: string;
  cssSelector: string;
}

interface Resp<T> {
  data: T[];
  error: string;
  status: number;
}

const { data, error, status, refresh } = await useFetch<Resp<Trigger>>(
  `/${schema}/api/trigger`
);

const showSidePanel = ref(false);
const showDeleteConfirm = ref(false);

async function saveTrigger() {
  const createAction = {
    name: formTrigger.value.name,
    cssSelector: formTrigger.value.cssSelector,
  };
  await $fetch(`/${schema}/api/trigger`, {
    method: "POST",
    body: JSON.stringify(createAction),
  }).catch((error) => {
    console.error(error);
    formError.value = `Failed to add trigger (${error})`;
  });

  clearForm();
  showSidePanel.value = false;
  refresh();
}

async function executeDelete() {
  console.log("executeDelete", formTrigger.value);
  const resp = await $fetch(
    `/${schema}/api/trigger/${formTrigger.value.name}`,
    {
      method: "DELETE",
    }
  ).catch((error) => {
    console.error(error);
    formError.value = `Failed to delete trigger (${error})`;
  });
  console.log("delete resp", resp);
  refresh();
  showDeleteConfirm.value = false;
}

function clearForm() {
  formTrigger.value.name = "";
  formTrigger.value.cssSelector = "";

  formError.value = null;
}

const formTrigger = ref<Trigger>({
  name: "",
  cssSelector: "",
});

function addTrigger() {
  clearForm();
  showSidePanel.value = true;
}

function editTrigger(trigger: Trigger) {
  clearForm();
  formTrigger.value = { ...trigger };
  showSidePanel.value = true;
}

function deleteTrigger(trigger: Trigger) {
  console.log("delete", trigger);
  clearForm();
  formTrigger.value = { ...trigger };
  showDeleteConfirm.value = true;
}

const formError = ref<string | null>(null);
</script>

<template>
  <Container>
    <PageHeader title="Analytics" />
    <ContentBlock
      class="mt-1 w-9/12 mx-auto"
      title="Triggers"
      description="Trigger that fire analytics events"
    >
      <Button type="outline" size="small" @click="addTrigger">Add</Button>
      <CardList>
        <CardListItem v-for="trigger in data">
          <article class="py-5 lg:px-12.5 p-5">
            <header class="flex md:flex-row gap-3 items-start md:items-center">
              <div class="md:basis-2/5 py-2">
                <h2 class="text-heading-2xl">
                  {{ trigger.name }}
                </h2>
              </div>
              <Button type="outline" size="small" @click="editTrigger(trigger)"
                >edit</Button
              >
              <Button
                type="outline"
                size="small"
                icon="trash"
                @click="deleteTrigger(trigger)"
                >remove</Button
              >
            </header>
            <div class="md:flex md:basis-3/5">
              <p class="text-body-base">
                {{ trigger.cssSelector }}
              </p>
            </div>
          </article>
        </CardListItem>
      </CardList>
    </ContentBlock>
  </Container>

  <SideModal
    :show="showSidePanel"
    :fullScreen="true"
    :slideInRight="true"
    buttonAlignment="left"
    @close="showSidePanel = false"
  >
    <ContentBlockModal title="Trigger">
      <form @submit.prevent.default="">
        <InputLabel for="name" label="Name" />
        <InputString id="name" placeholder="Name" v-model="formTrigger.name" />
        <InputLabel for="cssSelector" label="Css Selector" />
        <InputString
          id="cssSelector"
          placeholder="Css Selector"
          v-model="formTrigger.cssSelector"
        />
      </form>
    </ContentBlockModal>
    <template #footer>
      <Button type="primary" size="medium" @click="saveTrigger">Save</Button>
      <div v-if="formError" class="text-menu p-4">{{ formError }}</div>
    </template>
  </SideModal>

  <SideModal
    :show="showDeleteConfirm"
    :fullScreen="false"
    :slideInRight="true"
    buttonAlignment="left"
    @close="showDeleteConfirm = false"
  >
    <ContentBlockModal title="Trigger">
      <p>Are you sure you want to delete this trigger?</p>
    </ContentBlockModal>
    <template #footer>
      <Button type="primary" size="medium" @click="executeDelete"
        >Delete</Button
      >
      <div v-if="formError" class="text-menu p-4">{{ formError }}</div>
    </template>
  </SideModal>
</template>
