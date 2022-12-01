<script setup lang="ts">
defineProps<{
    title: string;
    description?: string;
    headers: {id: string,label: string}[]
    rows: Record<string, string>[]
}>();
</script>

<template>
    <ContentBlock :title="title" :description="description">
        <!-- <ButtonGroup class="flex mb-5 flex-wrap">
            <div class="grow">
                <Button label="All 299 variables" type="tertiary" size="medium" />
            </div>
            <div class="relative">
                <label class="block absolute text-body-xs top-2 left-6 pointer-events-none" for="sort-by">
                    Sort by
                </label>
                <select name="sort-by"
                    class=" h-14 border border-gray-400 pb-2 pt-6 pl-6 pr-12 rounded-full appearance-none hover:bg-gray-100 hover:cursor-pointer bg-none">
                    <option v-for="header in headers" value="headers.id:">{{header.label}}</option>
                </select>
                <span class="absolute right-5 top-5 pointer-events-none">
                    <BaseIcon name="caret-down" :width="20" />
                </span>
            </div>
        </ButtonGroup> -->
   
        <Table>
            <template #head>
                <TableHeadRow>
                    <TableHead v-for="header in headers">
                        <span class="hidden sm:table-cell">{{header.label}}</span>
                    </TableHead>
                    <TableHead><span class="hidden sm:table-cell"></span></TableHead>
                </TableHeadRow>
            </template>
            <template #body>
                <NuxtLink custom  v-for="row in rows" :to="row._path">
                    <TableRow>
                        <TableCell>
                            {{ row[headers[0].id] }}
                            <dl class="font-normal sm:hidden text-gray-900">
                                <template v-for="header in headers.slice(1)">
                                    <dt class="font-bold mt-2.5">{{ header.label}}</dt>
                                    <dd>{{ row[header.id] }}</dd>
                                </template>         
                            </dl>
                        </TableCell>
                        <TableCell v-for="header in headers.slice(1)" class="hidden sm:table-cell">{{ row[header.id] }}</TableCell>
                        <TableCell class="hidden sm:table-cell">
                            <IconButton icon="arrow-right" class="text-blue-500" />
                        </TableCell>
                    </TableRow>
                </NuxtLink>

            </template>
        </Table>
        <!-- <Pagination :currentPage="45" :totalPages="55" /> -->
    </ContentBlock>
</template>
