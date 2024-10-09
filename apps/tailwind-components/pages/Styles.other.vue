<template>
  <h1 class="text-heading-3xl">Theme Styles</h1>

  <h2 class="text-heading-2xl py-4">Colors</h2>

  <h3 class="text-heading-1xl py-3">blue</h3>
  <div class="flex flex-wrap -mb-4 mt-6">
    <ColorTile color="blue-50" type="background" />
    <ColorTile color="blue-100" type="background" />
    <ColorTile color="blue-200" type="background" />
    <ColorTile color="blue-300" type="background" />
    <ColorTile color="blue-500" type="background" />
    <ColorTile color="blue-700" type="background" />
    <ColorTile color="blue-800" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">gray</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="gray-100" type="background" />
    <ColorTile color="gray-200" type="background" />
    <ColorTile color="gray-400" type="background" />
    <ColorTile color="gray-600" type="background" />
    <ColorTile color="gray-900" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">gray</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="gray-100" type="background" />
    <ColorTile color="gray-200" type="background" />
    <ColorTile color="gray-400" type="background" />
    <ColorTile color="gray-600" type="background" />
    <ColorTile color="gray-900" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">yellow</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="yellow-200" type="background" />
    <ColorTile color="yellow-500" type="background" />
    <ColorTile color="yellow-800" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">green</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="green-500" type="background" />
    <ColorTile color="green-800" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">orange</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="orange-500" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">red</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="red-500" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">Background colors</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="button-primary" type="background" />
    <ColorTile color="button-primary" type="background" />
    <ColorTile color="button-primary-hover" type="background" />
    <ColorTile color="button-secondary" type="background" />
    <ColorTile color="button-secondary-hover" type="background" />
    <ColorTile color="button-tertiary" type="background" />
    <ColorTile color="button-tertiary-hover" type="background" />
    <ColorTile color="button-outline" type="background" />
    <ColorTile color="button-outline-hover" type="background" />
    <ColorTile color="button-disabled" type="background" />
    <ColorTile color="button-disabled-hover" type="background" />
    <ColorTile color="search-button" type="background" />
    <ColorTile color="search-button-hover" type="background" />
    <ColorTile color="navigation" type="background" />
    <ColorTile color="search-results-view-tabs" type="background" />
    <ColorTile color="search-filter-group-toggle" type="background" />
    <ColorTile color="app-wrapper" type="background" />
    <ColorTile color="pagination" type="background" />
    <ColorTile color="pagination-hover" type="background" />
    <ColorTile color="footer" type="background" />
    <ColorTile color="modal-footer" type="background" />
    <ColorTile color="tab" type="background" />
    <ColorTile color="tab-hover" type="background" />
    <ColorTile color="tab-active" type="background" />
    <ColorTile color="disabled" type="background" />
    <ColorTile color="invalid" type="background" />
  </div>

  <h3 class="text-heading-1xl py-3">Text colors</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="button-primary" type="text" />
    <ColorTile color="button-primary-hover" type="text" />
    <ColorTile color="button-secondary" type="text" />
    <ColorTile color="button-secondary-hover" type="text" />
    <ColorTile color="button-tertiary" type="text" />
    <ColorTile color="button-tertiary-hover" type="text" />
    <ColorTile color="button-outline" type="text" />
    <ColorTile color="button-outline-hover" type="text" />
    <ColorTile color="button-disabled" type="text" />
    <ColorTile color="button-disabled-hover" type="text" />
    <ColorTile color="menu" type="text" />
    <ColorTile color="sub-menu" type="text" />
    <ColorTile color="sub-menu-hover" type="text" />
    <ColorTile color="breadcrumb-arrow" type="text" />
    <ColorTile color="breadcrumb" type="text" />
    <ColorTile color="title" type="text" />
    <ColorTile color="title-contrast" type="text" />
    <ColorTile color="sub-title-contrast" type="text" />
    <ColorTile color="search-button" type="text" />
    <ColorTile color="search-button-hover" type="text" />
    <ColorTile color="favorite" type="text" />
    <ColorTile color="favorite-hover" type="text" />
    <ColorTile color="search-results-view-tabs" type="text" />
    <ColorTile color="search-results-view-tabs-hover" type="text" />
    <ColorTile color="search-filter-title" type="text" />
    <ColorTile color="search-filter-expand" type="text" />
    <ColorTile color="search-filter-expand-mobile" type="text" />
    <ColorTile color="search-filter-group-title" type="text" />
    <ColorTile color="search-filter-group-title-mobile" type="text" />
    <ColorTile color="search-filter-group-checkbox" type="text" />
    <ColorTile color="search-filter-group-toggle" type="text" />
    <ColorTile color="search-filter-title-inverted" type="text" />
    <ColorTile color="search-filter-expand-inverted" type="text" />
    <ColorTile color="search-filter-group-title-inverted" type="text" />
    <ColorTile color="search-filter-group-checkbox-inverted" type="text" />
    <ColorTile color="search-filter-group-toggle-inverted" type="text" />
    <ColorTile color="tooltip-hover-dark" type="text" />
    <ColorTile color="tooltip-hover-light" type="text" />
    <ColorTile color="pagination" type="text" />
    <ColorTile color="pagination-input" type="text" />
    <ColorTile color="pagination-hover" type="text" />
    <ColorTile color="footer-link" type="text" />
    <ColorTile color="invalid" type="text" />
    <ColorTile color="valid" type="text" />
    <ColorTile color="disabled" type="text" />
    <ColorTile color="required" type="text" />
  </div>

  <h3 class="text-heading-1xl py-3">Border colors</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="button-primary" type="border" />
    <ColorTile color="button-primary-hover" type="border" />
    <ColorTile color="button-secondary" type="border" />
    <ColorTile color="button-secondary-hover" type="border" />
    <ColorTile color="button-tertiary" type="border" />
    <ColorTile color="button-tertiary-hover" type="border" />
    <ColorTile color="button-outline" type="border" />
    <ColorTile color="button-outline-hover" type="border" />
    <ColorTile color="button-disabled" type="border" />
    <ColorTile color="button-disabled-hover" type="border" />
    <ColorTile color="menu-active" type="border" />
    <ColorTile color="search-button" type="border" />
    <ColorTile color="search-input" type="border" />
    <ColorTile color="search-input-mobile" type="border" />
    <ColorTile color="pagination" type="border" />
    <ColorTile color="checkbox" type="border" />
    <ColorTile color="invalid" type="border" />
    <ColorTile color="valid" type="border" />
    <ColorTile color="disabled" type="border" />
  </div>

  <h3 class="text-heading-1xl py-3">Border radius</h3>
  <div class="flex flex-wrap mb-4 mt-6">
    <ColorTile color="button-primary" type="radius" />
    <ColorTile color="3px" type="radius" />
    <ColorTile color="50px" type="radius" />
    <ColorTile color="textarea-input" type="radius" />
    <ColorTile color="search-input" type="radius" />
    <ColorTile color="search-button" type="radius" />
    <ColorTile color="pagination" type="radius" />
    <ColorTile color="landing" type="radius" />
  </div>
</template>
