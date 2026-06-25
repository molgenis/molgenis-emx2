export const getContainers = `query getContainers($filter:ContainersFilter) {
    Containers(filter:$filter) {
        
        # Containers
        name
        description
        mg_tableclass
        
        # Developer pages
        html
        css
        javascript
        dependencies {
            mg_tableclass
            name
            url
            defer
            async
            fetchPriority {
                name
            }
        }
        enableBaseStyles
        enableButtonStyles
        enableFullScreen
        
        # Configurable pages
        blockOrder(orderby: { order: ASC } ) {
            id
            order
            block {
                id
                enableFullScreenWidth
                mg_tableclass
                
                # page headings
                title
                subtitle
                backgroundImage {
                    image {
                        id
                        url
                    }
                }
                titleIsCentered
                
                # components
                componentOrder(orderby: {order:ASC}) {
                    order
                    component {
                        id
                        mg_tableclass
                        
                        # TextElements
                        text
                        
                        # Headings
                        level
                        headingIsCentered
                        
                        # Paragraphs
                        paragraphIsCentered
                        
                        # images
                        displayName
                        image {
                            id
                            size
                            filename
                            extension
                            url
                        }
                        alt
                        width
                        height
                        imageIsCentered
                        
                        # navigation groups and cards
                        links {
                            id
                            title
                            description
                            url
                            urlLabel
                            urlIsExternal
                            order
                        }
                        
                    }
                }
            }
        }
    }
    _schema {
      id
      label
      tables {
        id
        schemaId
        name
        label
        description
        tableType
        columns {
          columnType
          id
          label
          section
          heading
          computed
          description
          formLabel
          key
          position
          refBackId
          refLabel
          refLabelDefault
          refLinkId
          refSchemaId
          refTableId
          required
          validation
          visible
          table
          name
          inherited
          defaultValue
        }
      }
    }
}`;

export const getContainersAndMetadata = `{
    Containers (orderby: { name: ASC }) {
        name
        mg_tableclass
    }
    _schema {
        id
        label
        tables {
          id
          schemaId
          name
          label
          description
          tableType
          columns {
            columnType
            id
            label
            section
            heading
            computed
            description
            formLabel
            key
            position
            refBackId
            refLabel
            refLabelDefault
            refLinkId
            refSchemaId
            refTableId
            required
            validation
            visible
            table
            name
            inherited
            defaultValue
          }
        }
    }
}`;
