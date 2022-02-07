<template>
  <div id="app" class="d-flex">
    <div id="sidebar-wrapper" class="border-right">
      <div class="sidebar-heading">Components</div>
      <div class="list-group list-group-flush">
        <li class="list-group-item"><strong>Display</strong></li>
        <a href="#page-header" class="list-group-item">Page header</a>
        <a href="#links-list" class="list-group-item">Links list</a>
        <a href="#grid-block" class="list-group-item">Grid Block</a>
        <a href="#key-value-block" class="list-group-item">Key value block</a>
        <a href="#table-display" class="list-group-item">Table display</a>
        <li class="list-group-item"><strong>Layout</strong></li>
        <a href="#read-more" class="list-group-item">Read more</a>
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
import DemoItem from "./DemoItem.vue";

export default {
  components: {
    DemoItem,
  },
};
</script>


