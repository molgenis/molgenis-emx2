<template>
  <div id="app" class="d-flex">
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
        <li class="list-group-item"><strong>Table</strong></li>
         <a href="#table-display" class="list-group-item">Table display</a>
        <a href="#pagination" class="list-group-item">Pagination</a>
      </div>
    </div>

    <div id="page-content-wrapper" class="m-3 container-fluid">
      <demo-item id="page-header" label="Page Header">
        <label class="font-italic">basic header with a title</label>
        <page-header title="My title"></page-header>

        <label class="font-italic">header with title and subtitle</label>
        <page-header title="My title" subTitle="and subtitle"></page-header>

        <label class="font-italic">Header with subtitle and logo</label>
        <page-header
          title="My title"
          subTitle="and subtitle"
          logoUrl="https://avatars.githubusercontent.com/u/1688158?s=200&v=4"
        ></page-header>
      </demo-item>

      <demo-item id="links-list" label="Links list">
        <label class="font-italic">basic list of links</label>
        <links-list
          :items="[
            { label: 'Molgenis', href: 'https://molgenis.org' },
            { label: 'Google', href: 'https://google.com' },
          ]"
        ></links-list>
      </demo-item>

      <demo-item id="grid-block" label="Grid Block">
        <label class="font-italic">example</label>
        <grid-block heading="block heading"></grid-block>
      </demo-item>

      <demo-item id="image-display" label="Grid Block">
        <label class="font-italic">clickable image</label>
        <div>
          <image-display
            :isClickable="true"
            @clicked="showAlert(JSON.stringify($event))"
            id="my-image"
            url="data:image/png;base64,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"
          ></image-display>
        </div>
      </demo-item>

      <demo-item id="key-value-block" label="Key value block">
        <label class="font-italic"
          >single item example ( label is hidden or used as heading )</label
        >
        <key-value-block
          heading="my heading"
          :items="[{ key: 'key1', label: 'label 1', value: 'value 1' }]"
        ></key-value-block>

        <label class="font-italic">multiple items example</label>
        <key-value-block
          heading="my heading"
          :items="[
            { key: 'key1', label: 'label 1', value: 'value 1' },
            { key: 'key2', label: 'label 2', value: 'value 2' },
          ]"
        ></key-value-block>
      </demo-item>

      <demo-item id="read-more" label="Read More">
        <label class="font-italic">large text</label>
        <read-more
          text="I'm baby food truck chia asymmetrical venmo, crucifix hexagon whatever. +1 hammock live-edge la croix mumblecore semiotics. Brunch mumblecore vape, cardigan leggings iceland blue bottle single-origin coffee migas authentic typewriter polaroid slow-carb seitan. Chicharrones normcore YOLO, XOXO occupy bicycle rights vexillologist schlitz. Quinoa yuccie typewriter, raw denim offal helvetica williamsburg jean shorts cred roof party affogato post-ironic pour-over."
        ></read-more>
      </demo-item>

      <demo-item id="table-display" label="Table Display">
        <label class="font-italic">example</label>

        <table-display
          :columns="[
            { name: 'firstName', label: 'First name' },
            { name: 'lastName', label: 'Sir name' },
            { name: 'occupation', label: 'Occupation' },
          ]"
          :rows="[
            {
              firstName: 'Albus',
              lastName: 'Dumbledore',
              occupation: 'Headmaster',
            },
            { firstName: 'Rubeus', occupation: 'Grounds keeper' },
            { lastName: 'Snape', occupation: 'Professor' },
          ]"
        ></table-display>
      </demo-item>

      <demo-item id="pagination" label="Pagination">
        <pagination v-model="examples.pagination.page" :count="250" />
        <div>page number: {{ examples.pagination.page }}</div>
      </demo-item>
    </div>
  </div>
</template>

<style scoped>
#sidebar-wrapper .sidebar-heading {
  padding: 0.875rem 1.25rem;
  font-size: 1.2rem;
}

#sidebar-wrapper .list-group {
  width: 15rem;
}

#page-content-wrapper {
  min-width: 75vw;
}
</style>

<script>
import Vue from "vue"
import DemoItem from "./DemoItem.vue";

export default {
  components: {
    DemoItem,
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


