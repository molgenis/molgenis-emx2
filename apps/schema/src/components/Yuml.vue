</script>
}
;
},
},
return res;

})
;
}
})
;
}
res += `[${table.name}]${column.name}-*>[${column.refTable}],`;

console.log(JSON.stringify(column));
} else
if (column.columnType === "REF_ARRAY") {
  res += `[${table.name}]${column.name}->[${column.refTable}],`;

  console.log(JSON.stringify(column));
  if (column.columnType === "REF") {
  .
    forEach((column) => {
    )
    this.showAttributes.includes("external")
  )
(c.refSchema == undefined ||
    !c.inherited &&
    (c)
=>
.filter(
    table.columns
if (Array.isArray(table.columns)) {
}
res += `[${table.inherit}]^-[${table.name}],`;
if (table.inherit) {
.
  forEach((table) => {
  )
  this.showAttributes.includes("external")
  t.externalSchema == undefined ||
  (t)
=>
.
  filter(
      this.tables
  // relations

}
)
;
}
res += `{bg:dodgerblue}],`;
} else
{
  res += `],`;
  if (table.externalSchema) {
  }
}
)
;
}］;`; //notice I use not standard [] to not break yuml
                  column.columnType.includes("ARRAY") ? "*" : "1"
                res += `［${column.required ? "1" : "0"}..$
{
}
res += `${column.name}:${column.columnType}`;
} else
{
  res += `${column.name}:${column.refTable}`;
  if (column.columnType.includes("REF")) {
  .
    forEach((column) => {
    .
      filter((column) => !column.inherited)
      table.columns
      res += "|";
    )
    {
      this.showAttributes.includes("attributes")
      Array.isArray(table.columns) &&
      if (

          res += `[${table.name}`;
    .
      forEach((table) => {
      )
      (t)
    =>
      !t.externalSchema || this.showAttributes.includes("external")
          .filter(
              this.tables
      // classes
      let res = "http://yuml.me/diagram/plain;dir:bt/class/";
      if (!this.tables) return "";
      this.loadingYuml = true;
      yuml()
      {
      }
    ,
      return this.schema.tables;
      tables()
      {
        computed: {
        }
      ,
      }
      ;
      showAttributes: [],
          imgFullscreen
    :
      false,
          loadingYuml
    :
      false,
      return {
        data() {
        },
        schema: Object,
        props: {},
        IconAction,
        InputCheckbox,
        Spinner,
        components: {
          export default {

                import {Spinner, InputCheckbox, IconAction} from "@mswertz/emx2-styleguide";
              < script >

              < /template>
              < /div>
              < /div>
      />
    @load
      = "loadingYuml = false"
      alt = "Small"
    }
      "
      'max-width'
    :
      imgFullscreen ? 'none' : '100%',
          'max-height'
    :
      imgFullscreen ? 'none' : '30%',
          visibility
    :
      loadingYuml ? 'hidden' : 'visible',
    :
      style = "{
    :
      src = "yuml"
    :
      key = "JSON.stringify(showAttributes)"
      < img
      >
      style = "text-align: center; overflow: auto"
      v - scroll - lock = "imgFullscreen"
          < div
          < Spinner
      v -
      if= "loadingYuml" / >
          / >
          :
      options = "['attributes', 'external']"
    :
      defaultValue = "showAttributes"
      v - model = "showAttributes"
      < InputCheckbox
      / -- >
      @click = "imgFullscreen = !imgFullscreen"

      class

      = "fullscreen-icon"
    :
      icon = "imgFullscreen ? 'compress' : 'expand'"
      v -
      if= "!loadingYuml"
          < !--IconAction
          < div v -
      if= "schema.tables" >
          < template >
