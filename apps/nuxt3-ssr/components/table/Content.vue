<script setup lang="ts">
const props = defineProps<{
    title: string;
    description?: string;
    headers?: {}[]
}>();
</script>

<template>
    <ContentBlock :title="title" :description="description">
        <ButtonGroup class="flex mb-5 flex-wrap">
            <div class="grow">
                <Button label="All 299 variables" type="tertiary" size="medium" />
            </div>
            <div class="relative">
                <label class="block absolute text-body-xs top-2 left-6 pointer-events-none" for="sort-by">
                    Sort by
                </label>
                <select name="sort-by"
                    class=" h-14 border border-gray-400 pb-2 pt-6 pl-6 pr-12 rounded-full appearance-none hover:bg-gray-100 hover:cursor-pointer bg-none">
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
                            <dt class="font-bold mt-2.5">Start & end year</dt>
                            <dd>2017 - Ongoing</dd>
                        </dl>
                    </TableCell>
                    <TableCell class="hidden sm:table-cell">
                        Data collection at birth</TableCell>
                    <TableCell class="hidden sm:table-cell"> 2017 - Ongoing</TableCell>
                    <TableCell class="hidden sm:table-cell">
                        <IconButton icon="arrow-right" class="text-blue-500" />
                    </TableCell>
                </TableRow>
            </template>
        </Table>
        <Pagination :currentPage="45" :totalPages="55" />
    </ContentBlock>
</template>
