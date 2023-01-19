<script setup>
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
    <ButtonGroup class="flex mb-5 flex-wrap">
      <div class="grow">
        <Button label="All 299 variables" type="tertiary" size="medium" />
      </div>
      <div class="relative">
        <label
          class="block absolute text-body-xs top-2 left-6 pointer-events-none"
          for="sort-by"
        >
          Sort by
        </label>
        <select
          name="sort-by"
          class="h-14 border border-gray-400 pb-2 pt-6 pl-6 pr-12 rounded-full appearance-none hover:bg-gray-100 hover:cursor-pointer bg-none"
        >
          <option value="name">Name</option>
          <option value="description">Description</option>
          <option value="start-end-year">Start & end year</option>
        </select>
        <span class="absolute right-5 top-5 pointer-events-none">
          <BaseIcon name="caret-down" :width="20" />
        </span>
      </div>
    </ButtonGroup>
    <Table>
      <template #head>
        <TableHeadRow>
          <TableHead>
            <span class="hidden sm:table-cell">Name</span>
          </TableHead>
          <TableHead :fullWidth="true" class="hidden sm:table-cell">
            Description
          </TableHead>
          <TableHead :noWrap="true" class="hidden sm:table-cell">
            Start & end year
          </TableHead>
          <TableHead class="hidden sm:table-cell"></TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow>
          <TableCell>
            Birth
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at birth</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at birth</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            Inclusion
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at inclusion in LLNEXT</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at inclusion in LLNEXT</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            LLNEXT
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collected from LLNEXT partners</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collected from LLNEXT partners</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M1
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 1 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 1 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M2
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 2 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 2 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M3
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 3 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 3 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M4
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 4 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 4 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M5
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 5 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 5 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M9
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 6 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 6 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M10
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 9 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 9 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M11
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 10 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 10 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            M12
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 11 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 11 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            P12
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at infant's age of 12 month</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at infant's age of 12 month</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            P18
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at gestational age of 12 weeks</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at gestational age of 12 weeks</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            P28
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at gestational age of 18 weeks</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at gestational age of 18 weeks</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            P32
            <dl class="font-normal sm:hidden text-gray-900">
              <dt class="font-bold mt-2.5">Description</dt>
              <dd>Data collection at gestational age of 28 weeks</dd>
              <dt class="font-bold mt-2.5">Start &amp; end year</dt>
              <dd>2017 - Ongoing</dd>
            </dl>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            Data collection at gestational age of 28 weeks</TableCell
          >
          <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
          <TableCell class="hidden sm:table-cell"
            ><IconButton icon="arrow-right" class="text-blue-500"
          /></TableCell>
        </TableRow>
      </template>
    </Table>
    <Pagination :currentPage="45" :totalPages="55" />
  </ContentBlock>
</template>
