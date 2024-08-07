<template>
  <div class="flex">
    <div class="flex-1">
      <div>
        <BreadCrumbs
          current="Current location"
          :crumbs="{ Home: '/', Schema: '/schema', Page: '/schema/page' }"
        />
      </div>
    </div>
  </div>
</template>
