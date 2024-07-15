<script setup lang="ts">
interface Trigger {
  name: string;
  cssSelector: string;
  schema: string;
  app?: string;
}

interface Resp<T> {
  data: T[];
  error: string;
  status: number;
}

const devSchema = "pet store";

const { data, error, status } = await useFetch<Resp<Trigger>>(
  `/${devSchema}/api/trigger`
);

const showSidePanel = ref(false);

function addTrigger() {
  showSidePanel.value = true;
}

async function saveTrigger() {
  const createAction = {
    name: formTrigger.value.name,
    cssSelector: formTrigger.value.cssSelector,
  };
  const resp = await $fetch(`/${devSchema}/api/trigger`, {
    method: "POST",
    body: JSON.stringify(createAction),
  });
  if (resp.status === 200) {
    clearForm();
    showSidePanel.value = false;
  }
}

function clearForm() {
  formTrigger.value.name = "";
  formTrigger.value.cssSelector = "";
  formTrigger.value.schema = "";
  formTrigger.value.app = "";
}

const formTrigger = ref<Trigger>({
  name: "",
  cssSelector: "",
  schema: "",
  app: "",
});
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
            <TableHead>schema</TableHead>
            <TableHead>app</TableHead>
          </TableHeadRow>
        </template>

        <template #body>
          <TableRow v-for="trigger in data">
            <TableCell>{{ trigger.name }}</TableCell>
            <TableCell>{{ trigger.cssSelector }}</TableCell>
            <TableCell>{{ trigger.schema }}</TableCell>
            <TableCell>{{ trigger.app }}</TableCell>
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
        <InputLabel for="schema" label="Schema" />
        <InputString
          id="schema"
          placeholder="Schema"
          v-model="formTrigger.schema"
        />
        <InputLabel for="app" label="App" />
        <InputString id="app" placeholder="App" v-model="formTrigger.app" />
      </form>
    </ContentBlockModal>
    <template #footer>
      <Button type="primary" size="medium" @click="saveTrigger">Save</Button>
    </template>
  </SideModal>
</template>
