<script setup>
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import Table from "../../../../tailwind-components/app/components/Table.vue";
import TableCell from "../../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../../tailwind-components/app/components/TableRow.vue";
import IconButton from "../../../../tailwind-components/app/components/IconButton.vue";
defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <Table>
      <template #head>
        <TableHeadRow>
          <TableHead>Name</TableHead>
          <TableHead class="hidden sm:table-cell">Description</TableHead>
          <TableHead class="hidden sm:table-cell">Participants</TableHead>
          <TableHead class="hidden sm:table-cell">Ages</TableHead>
          <TableHead class="hidden sm:table-cell"></TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow>
          <TableCell
            ><a
              href="#"
              class="hover:underline hover:bg-link-hover text-body-base"
              >Children</a
            >
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>
                Children of pregnant women included in LLNEXT. They are followed
                until one year after birth.
              </dd>
              <dt class="font-bold mt-2.5">Participants</dt>
              <dd>1250</dd>
              <dt class="font-bold mt-2.5">Ages</dt>
              <dd>All ages</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Children of pregnant women included in LLNEXT. They are followed
            until one year after birth.
          </TableCell>
          <TableCell class="hidden sm:table-cell">1250</TableCell>
          <TableCell :allowLineBreak="true" class="hidden sm:table-cell"
            >All ages</TableCell
          >
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-link"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell
            ><a
              href="#"
              class="hover:underline hover:bg-link-hover text-body-base"
              >Partners</a
            >
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>
                Partners of pregnant women included in LLNEXT. They are asked to
                fill in three questionnaires, provide antropometic measures and
                donate a blood sample.
              </dd>
              <dt class="font-bold mt-2.5">Participants</dt>
              <dd>1250</dd>
              <dt class="font-bold mt-2.5">Ages</dt>
              <dd>All ages</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Partners of pregnant women included in LLNEXT. They are asked to
            fill in three questionnaires, provide antropometic measures and
            donate a blood sample.
          </TableCell>
          <TableCell class="hidden sm:table-cell">1250</TableCell>
          <TableCell :allowLineBreak="true" class="hidden sm:table-cell"
            >All ages</TableCell
          >
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-link"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell
            ><a
              href="#"
              class="hover:underline hover:bg-link-hover text-body-base"
              >Mothers</a
            >
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>
                Pregnant women included in LLNEXT. They are follwed from the
                third months of pregnancy until one year after delivery.
              </dd>
              <dt class="font-bold mt-2.5">Participants</dt>
              <dd>1250</dd>
              <dt class="font-bold mt-2.5">Ages</dt>
              <dd>All ages</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Pregnant women included in LLNEXT. They are follwed from the third
            months of pregnancy until one year after delivery.
          </TableCell>
          <TableCell class="hidden sm:table-cell">1250</TableCell>
          <TableCell :allowLineBreak="true" class="hidden sm:table-cell"
            >All ages</TableCell
          >
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-link"
          /></TableCell>
        </TableRow>
      </template>
    </Table>
  </ContentBlock>
</template>
