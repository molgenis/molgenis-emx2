interface IBuildAggregationProps {
  table: string;
  column?: string;
  method: string;
}

interface IBuildAggregationReturn {
  query: string;
  gqlTableName: string;
  gqlBodyIsNested: boolean;
}

export function buildAggregationQuery({
  table,
  column,
  method = "count",
}: IBuildAggregationProps): IBuildAggregationReturn {
  const gqlTable = `${table}_agg`;
  const output: IBuildAggregationReturn = {
    query: "",
    gqlTableName: gqlTable,
    gqlBodyIsNested: false,
  };

  let body = "";

  if (["_sum", "avg", "min", "max"].includes(method)) {
    output.gqlBodyIsNested = true;
    body = `${method} {
            ${column}
        }`;
  } else {
    body += "count";
  }

  output.query = `query getAggregation($filter: ${table}Filter) {
        ${table}_agg(filter: $filter) {
            ${body}
        }
    }`;

  return output;
}
