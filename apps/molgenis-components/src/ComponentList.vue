<template>
  
    <div id="page-content-wrapper" class="m-3 container-fluid">

      <demo-item id="read-more" label="Read More">
        <label class="font-italic">large text</label>
        <read-more
          text="I'm baby food truck chia asymmetrical venmo, crucifix hexagon whatever. +1 hammock live-edge la croix mumblecore semiotics. Brunch mumblecore vape, cardigan leggings iceland blue bottle single-origin coffee migas authentic typewriter polaroid slow-carb seitan. Chicharrones normcore YOLO, XOXO occupy bicycle rights vexillologist schlitz. Quinoa yuccie typewriter, raw denim offal helvetica williamsburg jean shorts cred roof party affogato post-ironic pour-over."
        ></read-more>
      </demo-item>

      <demo-item id="breadcrumb" label="Breadcrumb">
        <breadcrumb :crumbs="{'Home':'/', 'Schema':'/schema', 'Page':'/schema/page'}"/>
        <label>Example with nested</label>
        <breadcrumb :crumbs="{'Home':'/', 'Schema':'/schema', 'Page':'/schema/page'}"
            :dropdown="{'Other':'/other','Other2':'/other2'}"/>
      </demo-item>

      <demo-item id="pagination" label="Pagination">
        <pagination v-model="examples.pagination.page" :count="250" />
        <div>page number: {{ examples.pagination.page }}</div>
      </demo-item>

     <demo-item id="message-error" label="Message Error">
        <message-error>Something bad</message-error>
      </demo-item>
    </div>
  
</template>

<script>

import DemoItem from "./DemoItem.vue";
import ExplorerTable from "./components/tables/ExplorerTable.vue";

export default {
  components: {
    DemoItem,
    ExplorerTable
},
  data() {
    return {
      examples: {
        pagination: {
          page: 3
        }
      }
    };
  },
  methods: {
    showAlert(msg) {
      alert(msg);
    },
  },
};
</script>


