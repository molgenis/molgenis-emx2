<template>
  <div id="sidebar-wrapper" class="border-right">
    <div class="sidebar-heading">Components</div>
    <div class="list-group list-group-flush">
      <li class="list-group-item"><strong>Display</strong></li>
      <a href="#page-header" class="list-group-item">Page header</a>
      <a href="#links-list" class="list-group-item">Links list</a>
      <a href="#grid-block" class="list-group-item">Grid Block</a>
      <a href="#image-display" class="list-group-item">Image Display</a>
      <a href="#key-value-block" class="list-group-item">Key value block</a>
      <a href="#table-display" class="list-group-item">Table display</a>
      <li class="list-group-item"><strong>Layout</strong></li>
      <a href="#read-more" class="list-group-item">Read more</a>
      <a href="#breadcrumb" class="list-group-item">Breadcrumb</a>
      <li class="list-group-item"><strong>Table</strong></li>
      <a href="#table-display" class="list-group-item">Table display</a>
      <a href="#pagination" class="list-group-item">Pagination</a>
      <li class="list-group-item"><strong>Forms</strong></li>
      <a href="#message-error" class="list-group-item">Message Error</a>
      <li class="list-group-item"><strong>Table data Client</strong></li>
      <router-link to="/client" class="list-group-item">Client</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: "Sidebar",
};
</script>
