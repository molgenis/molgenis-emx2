<script setup>
import ContentBlock from "./ContentBlock.vue";
import Table from "./Table.vue";
import TableRow from "./TableRow.vue";
import TableHead from "./TableHead.vue";
import TableCell from "./TableCell.vue";

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
        <TableRow>
          <TableHead> Name </TableHead>
          <TableHead> Description </TableHead>
          <TableHead> Start & end year </TableHead>
        </TableRow>
      </template>
      <template #body>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
        <TableRow>
          <TableCell> Birth </TableCell>
          <TableCell> Data collection at inclusion in LLNEXT </TableCell>
          <TableCell> 2017 &ndash; Ongoing</TableCell>
        </TableRow>
      </template>
    </Table>
  </ContentBlock>
</template>
