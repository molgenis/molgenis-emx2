export const getContainersQuery = `query getContainers($filter:ContainersFilter) {
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
        
        # Configurable pages: base block info
        blocks {
            ...BlocksAllFields2
        }
            
        # Configurable pages: ordered for page rendering
        blockOrder(orderby: { order: ASC } ) {
            id
            order
            block {
                id
                mg_tableclass
                
                # ui settings for blocks: settings
                columns
                enableFullScreenWidth
                applyShadedBackground
                
                # page headings
                title
                subtitle
                backgroundImage {
                    displayName
                    image {
                        id
                        url
                    }
                    alt
                    width
                    height
                    imageIsCentered
                    id
                }
                titleIsCentered
                pageHeaderHeight {
                    name
                }
                
                # components
                componentOrder(orderby: {order:ASC}) {
                    id
                    order
                    component {
                        id
                        mg_tableclass
                        
                        # TextElements
                        text
                        
                        # Headings
                        level
                        headingIsCentered
                        headingIsHidden
                        
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
                        
                        # Files
                        file {
                            id
                            size
                            filename
                            extension
                            url
                        }
                        alternateFileName
                        fileTag
                        linkToExternalFile
                        fileIsAnExternalLink
                        
                        # Filelist
                        showFilesWithTag

                        #Buttons
                        buttonLabel
                        buttonLink
                        buttonType
                        buttonSize
                        buttonIsCentered

                        # navigation cards
                        id
                        title
                        description
                        url
                        urlLabel
                        urlIsExternal
                        
                        # lists: unordered and ordered
                        orderedItems
                        unorderedItems
                        
                        # statistical charts
                        chartType {
                            name
                        }
                        chartTitle
                        chartDescription
                        chartData {
                            id
                            xValue
                            xLabel
                            yValue
                            yLabel
                            fillColor
                            strokeColor
                            sortOrder
                            primaryGroupValue
                            primaryGroupLabel
                            secondaryGroupValue
                            secondaryGroupLabel
                            
                            # map data
                            locationName
                            alternateId
                            city
                            country
                            continent
                            latitude
                            longitude
                            website
                            tooltipContent
                        }
                        legendIsEnabled
                        legendPosition {
                            name
                        }
                        legendIsHorizontal
                        legendMarkerType {
                            name
                        }
                        fillColor
                        strokeColor
                        hoverFillColor
                        hoverStrokeColor
                        colorPalette {
                            value
                            color
                        }
                        chartWidth
                        chartHeight
                        topMargin
                        rightMargin
                        bottomMargin
                        leftMargin
                        enableAnimations
                        enableHoverEvents
                        enableClickEvents
                        x
                        y
                        primaryGroupValues
                        secondaryGroupValues
                        xAxisTitle
                        xAxisMinValue
                        xAxisMaxValue
                        xAxisTicks
                        breakXAxisTickLabelsAt
                        yAxisTitle
                        yAxisMinValue
                        yAxisMaxValue
                        yAxisTicks
                        breakYAxisTickLabelsAt
                        
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
