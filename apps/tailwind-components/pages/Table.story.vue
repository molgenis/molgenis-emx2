<template>
  <div class="flex">
    <div class="flex-1">
      <Table>
        <template v-slot:head> Head </template>
        <template v-slot:body>
          <TableHeadRow>
            <TableCell> label </TableCell>
            <TableCell> row 1 </TableCell>
            <TableCell> row 2 </TableCell>
          </TableHeadRow>
          <TableRow>
            <TableCell> items </TableCell>
            <TableCell> item 1</TableCell>
            <TableCell> item 2</TableCell>
          </TableRow>
          <TableRow>
            <TableCell> items </TableCell>
            <TableCell> item 3</TableCell>
            <TableCell> item 4</TableCell>
          </TableRow>
        </template>
        <template v-slot:footer> Footer </template>
      </Table>
    </div>
  </div>
</template>
