<template>
  <div class="flex">
    <div class="flex items-center h-16">
      <HarmonizationStatusIcon status="unmapped" class="mr-1" /> No Data
      <HarmonizationStatusIcon status="complete" class="mr-1 ml-6" /> Completed
      <HarmonizationStatusIcon status="partial" class="mr-1 ml-6" /> Partial
      <a class="ml-6 mr-4 hover:underline text-blue-500 cursor-help"
        >About statuses</a
      >
    </div>
  </div>
</template>
