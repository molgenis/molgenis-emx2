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

const { data, refresh } = await useFetch<Trigger[]>(`/${schema}/api/trigger`);

const triggers = computed(() => data?.value?.toReversed());

const showAddModal = ref(false);
const showEditModal = ref(false);
const showDeleteConfirm = ref(false);

async function saveTrigger() {
  const createAction = {
    name: formTrigger.value.name,
    cssSelector: formTrigger.value.cssSelector,
  };
  const resp = await $fetch(`/${schema}/api/trigger`, {
    method: "POST",
    body: JSON.stringify(createAction),
  }).catch((error) => {
    console.error(error);
    formError.value = `Failed to add trigger (${error})`;
  });

  if (resp.status === "SUCCESS") {
    clearForm();
    showAddModal.value = false;
    refresh();
  }
}

async function updateTrigger() {
  const updateAction = {
    cssSelector: formTrigger.value.cssSelector,
  };
  const resp = await $fetch(
    `/${schema}/api/trigger/${formTrigger.value.name}`,
    {
      method: "PUT",
      body: JSON.stringify(updateAction),
    }
  ).catch((error) => {
    console.error(error);
    formError.value = `Failed to update trigger (${error})`;
  });

  if (resp.status === "SUCCESS") {
    clearForm();
    showEditModal.value = false;
    refresh();
  }
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

  if (resp.status === "SUCCESS") {
    clearForm();
    showDeleteConfirm.value = false;
    refresh();
  }
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
  showAddModal.value = true;
}

function editTrigger(trigger: Trigger) {
  clearForm();
  formTrigger.value = { ...toRaw(trigger) };
  showEditModal.value = true;
}

function deleteTrigger(trigger: Trigger) {
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
        <CardListItem v-for="trigger in triggers">
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
    :show="showAddModal"
    :fullScreen="true"
    :slideInRight="true"
    buttonAlignment="left"
    @close="showAddModal = false"
  >
    <ContentBlockModal title="Trigger">
      <form @submit.prevent.default="" class="flex flex-col gap-3">
        <div>
          <InputLabel for="name" class="">Name</InputLabel>
          <InputString
            id="name"
            placeholder="Name"
            v-model="formTrigger.name"
          />
        </div>
        <div>
          <InputLabel for="cssSelector">Css Selector</InputLabel>
          <InputTextArea
            id="cssSelector"
            placeholder="Css Selector"
            v-model="formTrigger.cssSelector"
          />
        </div>
      </form>
    </ContentBlockModal>
    <template #footer>
      <Button type="primary" size="medium" @click="saveTrigger">Save</Button>
      <div v-if="formError" class="text-menu p-4">{{ formError }}</div>
    </template>
  </SideModal>

  <SideModal
    :show="showEditModal"
    :fullScreen="true"
    :slideInRight="true"
    buttonAlignment="left"
    @close="showEditModal = false"
  >
    <ContentBlockModal :title="`Update Trigger: ${formTrigger.name}`">
      <form @submit.prevent.default="">
        <InputLabel for="cssSelector">Css Selector</InputLabel>
        <InputTextArea
          id="cssSelector"
          placeholder="Css Selector"
          v-model="formTrigger.cssSelector"
        />
      </form>
    </ContentBlockModal>
    <template #footer>
      <Button type="primary" size="medium" @click="updateTrigger"
        >Update</Button
      >
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
