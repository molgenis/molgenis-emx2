<template>
  <demo-item id="breadcrumb" label="Breadcrumb">
    <breadcrumb
      :crumbs="{ Home: '/', Schema: '/schema', Page: '/schema/page' }"
    />
    <label>Example with nested</label>
    <breadcrumb
      :crumbs="{ Home: '/', Schema: '/schema', Page: '/schema/page' }"
      :dropdown="{ Other: '/other', Other2: '/other2' }"
    />
  </demo-item>
</template>

