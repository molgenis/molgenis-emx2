<script setup lang="ts">
const { data, error, status } = await useFetch<Resp<Trigger>>(
  "/pet store/api/trigger"
);
</script>

<template>
  <Container>
    <PageHeader title="Analytics" />
    {{ data }}
    <ContentBlock
      class="mt-1"
      title="Triggers"
      description="Trigger that fire analytics events"
    >
      <Table>
        <template #head>
          <Button @click="navigateTo('/create')" type="secondary">Add</Button>
          <TableHeadRow>
            <TableHead>name</TableHead>
            <TableHead>css locator</TableHead>
            <TableHead>schema</TableHead>
            <TableHead>app</TableHead>
          </TableHeadRow>
        </template>

        <template #body>
          <TableRow
            v-for="trigger in data"
            @click="navigateTo(`/${database.id}`)"
          >
            <TableCell>{{ trigger.name }}</TableCell>
            <TableCell>{{ trigger.cssSelector }}</TableCell>
            <TableCell>{{ trigger.schema }}</TableCell>
            <TableCell>{{ trigger.app }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
