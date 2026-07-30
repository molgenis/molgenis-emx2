const chartData = `{
  chartId
  chartType {
    name
  }
  chartTitle
  chartSubtitle
  xAxisLabel
  xAxisMinValue
  xAxisMaxValue
  xAxisTicks
  yAxisLabel
  yAxisMinValue
  yAxisMaxValue
  yAxisTicks
  topMargin
  rightMargin
  bottomMargin
  leftMargin
  legendPosition {
    name
  }
  dataPoints(
    orderby: [
      { primaryCategory: ASC }
      { secondaryCategory: ASC }
      { sortOrder: ASC }
    ]
  ) {
    id
    name
    value
    valueLabel
    series
    primaryCategory
    secondaryCategory
    primaryCategoryLabel
    secondaryCategoryLabel
    timeValue
    timeUnit {
      name
    }
    color
    description
    sortOrder
    locationName
    alternateId
    city
    country {
      name
    }
    continent {
      name
    }
    latitude
    longitude
    website
    tooltipContent
  }
}`;

export const chartQuery = `query getChart($filter: ChartsFilter) {
  Charts(filter: $filter) ${chartData}
}`;

export const dashboardQuery = `query getDashboardPage() {
  DashboardPage(filter: $filter) {
    name
    description
    charts ${chartData}
  }
}`;
