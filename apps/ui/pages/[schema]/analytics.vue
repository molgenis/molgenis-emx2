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

const { data, error, status } = await useFetch<Resp<Trigger>>(
  `/${schema}/api/trigger`
);

const showSidePanel = ref(false);

function addTrigger() {
  clearForm();
  showSidePanel.value = true;
}

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
  console.log(resp);
  if (resp.status === "SUCCESS") {
    clearForm();
    showSidePanel.value = false;
  } else {
    console.error(resp.error);
    formError.value = `Failed to add trigger`;
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

const formError = ref<string | null>(null);
</script>

<template>
  <Container>
    <PageHeader title="Analytics" />
    <ContentBlock
      class="mt-1"
      title="Triggers"
      description="Trigger that fire analytics events"
    >
      <Table>
        <template #head>
          <Button @click="addTrigger" type="secondary">Add</Button>
          <TableHeadRow>
            <TableHead>name</TableHead>
            <TableHead>css locator</TableHead>
          </TableHeadRow>
        </template>

        <template #body>
          <TableRow v-for="trigger in data">
            <TableCell>{{ trigger.name }}</TableCell>
            <TableCell>{{ trigger.cssSelector }}</TableCell>
          </TableRow>
        </template>
      </Table>
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
</template>
