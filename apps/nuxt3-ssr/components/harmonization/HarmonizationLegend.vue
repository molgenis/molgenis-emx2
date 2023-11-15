<template>
  <div class="flex">
    <div class="flex items-center h-16">
      <HarmonizationStatusIcon size="5" status="complete" class="mr-1 ml-6" />
      Completed
      <HarmonizationStatusIcon size="5" status="partial" class="mr-1 ml-6" />
      Partial
      <HarmonizationStatusIcon size="5" status="unmapped" class="mr-1 ml-6" />
      No Data
      <a class="ml-6 mr-8 hover:underline text-blue-500 cursor-help"
        >About statuses</a
      >
    </div>
  </div>
</template>
