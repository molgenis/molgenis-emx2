<template>
  <div class="overflow-auto" style="max-width: 100%">
    <Spinner v-if="!imageURL" />
    <img v-else :src="imageURL" />
  </div>
</template>
<script>
import mermaid from "mermaid";
import { Spinner } from "molgenis-components";

export default {
  components: {
    Spinner,
  },
  props: {
    tables: Array,
    showColumns: {
      type: Boolean,
      default: () => true,
    },
  },
  data() {
    return {
      svg: null,
      id: null,
      imageURL: null,
    };
  },
  methods: {
    async reload() {
      const config = {
        startOnLoad: false,
        theme: "base",
        themeVariables: {
          primaryTextColor: "black",
          lineColor: "black",
          primaryColor: "white",
          primaryBorderColor: "black",
        },
      };
      try {
        mermaid.initialize(config);
        let result = await mermaid.render(
          this.id,
          tablesDiagram(this.tables, this.showColumns)
        );
        this.svg = result.svg;

        //convert to PNG because word doesn't understand SVG
        const svgDataBase64 = btoa(unescape(encodeURIComponent(this.svg)));
        const svgDataUrl = `data:image/svg+xml;charset=utf-8;base64,${svgDataBase64}`;
        const svgImage = new Image();

        svgImage.onload = () => {
          const canvas = document.createElement("canvas");
          var parser = new DOMParser();
          var doc = parser.parseFromString(this.svg, "image/svg+xml");
          let viewbox = doc.getElementById(this.id).getAttribute("viewBox");
          canvas.width = Math.round(parseFloat(viewbox.split(" ")[2]));
          canvas.height = Math.round(parseFloat(viewbox.split(" ")[3]));
          const canvasCtx = canvas.getContext("2d");
          canvasCtx.drawImage(svgImage, 0, 0);
          this.imageURL = canvas.toDataURL("image/png");
        };
        svgImage.src = svgDataUrl;
      } catch (error) {
        console.log(error);
        this.error = error;
      }
    },
  },
  async mounted() {
    this.id = generateID(16);
    await this.reload();
  },
  watch: {
    async showColumns() {
      this.imageURL = null;
      await this.reload();
    },
  },
};

function generateID(length) {
  const letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  let uniqueID = "";
  while (uniqueID.length < length) {
    uniqueID += letters[Math.floor(Math.random() * letters.length)];
  }
  return uniqueID;
}

function tablesDiagram(tables, showColumns) {
  let graph = `
classDiagram
direction TB
`;
  tables.forEach((table) => {
    if (table.subclasses) {
      graph += `namespace ${table.name.replace(" ", "")}_hierarchy {\n`;
      graph += `  class \`${table.name}\`\n`;
      table.subclasses.forEach((subclass) => {
        graph += ` class \`${subclass.name}\`\n`;
      });
      graph += "}\n";
    } else {
      graph += `  class \`${table.name}\`\n`;
    }
    if (showColumns) {
      console.log(showColumns);
      table.columns.forEach((column) => {
        graph += `  \`${column.table}\`: +\`${column.name}\`\n`;
      });
    }
    if (table.subclasses) {
      table.subclasses.forEach((subclass) => {
        graph += ` \`${subclass.inherit}\` <|-- \`${subclass.name}\` \n`;
      });
    }
    table.columns.forEach((column) => {
      if (column.columnType === "REF" || column.columnType === "REF_ARRAY") {
        graph += `  class \`${column.refTable}\`\n`;
        graph += `  \`${column.refTable}\` <-- \`${column.table}\` : ${column.name} \n`;
      }
    });
  });
  return graph;
}
</script>
