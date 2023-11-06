<template>
  <div class="flex">
    <div class="flex items-center h-16">
      <HarmonizationStatusIcon status="unmapped" class="mr-1" /> Completed
      <HarmonizationStatusIcon status="complete" class="mr-1 ml-6" /> Partial
      <HarmonizationStatusIcon status="partial" class="mr-1 ml-6" /> No Data
      <a class="ml-6 mr-4 hover:underline text-blue-500 cursor-help"
        >About statuses</a
      >
    </div>
  </div>
</template>
