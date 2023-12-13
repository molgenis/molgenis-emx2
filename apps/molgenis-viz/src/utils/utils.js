// asDataObject
// Transform a dataset (i.e., array of objects) to an key-value object
// This is useful for preparing data for d3 visualisations such as pie charts
//
// @param data an array of objects
// @param key the property that contains an ID which values will be mapped to
// @param value property that will be mapped to the ID
//
// @examples
// data = [{'id': 'A1234', 'value': 123}, {'id': 'B1234', 'value': 634}]
// asDataObject(data, 'id', 'value')
// // {'A1234': 1234, 'B1234': 634}
//
// @return object
export function asDataObject(data, key, value) {
  const newDataObject = {};
  data.forEach((row) => {
    newDataObject[row[key]] = row[value];
  });
  return newDataObject;
}

// flattenData
// Flatten MOLGENIS API reponse
//
// @param data an array of objects with nested arrays/objects
// @return an array of objects with no nested arrays or objects
//
export function flattenData(data) {
  return data.map((row) => {
    const rowKeys = Object.keys(row);
    const newrow = {};
    rowKeys.map((key) => {
      if (row[key] instanceof Object) {
        if (row[key] instanceof Array) {
          const val = row[key].map((subrow) => subrow.value || subrow.name);
          newrow[key] = val.join(",");
        } else {
          newrow[key] = row[key].value;
        }
      } else {
        newrow[key] = row[key];
      }
    });
    return newrow;
  });
}

// Init Search Object
// Create object that will handle all loading messages (loading, error, successful)
// when querying for results via the MOLGENIS API
//
// @return object
export function initSearchResultsObject() {
  return {
    isSearching: false,
    wasSuccessful: false,
    hasFailed: false,
    errorMessage: null,
    successMessage: null,
    resultsUrl: null,
  };
}

// Minimum Data
// In an array of objects, return the earliest date in by named property

// @param data an array of objects
// @param dateVar name of attribute that contains the date value
//
// @param date; earliest date
export function minDate(data, dateVar) {
  return new Date(Math.min(...data.map((row) => new Date(row[dateVar]))));
}

// Minimum Data
// In an array of objects, return the most recent date in by named property
//
// @param data an array of objects
// @param dateVar name of attribute that contains the date value
//
// @return date; most recent date
export function maxDate(data, dateVar) {
  return new Date(Math.max(...data.map((row) => new Date(row[dateVar]))));
}

// Object To Url Filter Array
// Object containing  to an array of strings (in Molgenis format). Any value
// that is a comma-separated string, will be formatted accordingly
//
// @param object an object containing one more valid keys
//
// @examples
// const filters = {
//   gender: 'female',
//   age: null,
//   country: 'Australia, New Zealand'
//   group: null
// }
// const f = removeNullObjectKeys(filters)
// objectToUrlFilterArray(f)
// > ['gender==female', 'country=in=(Australia,New Zealand)']
//
// @return array of strings
export function objectToUrlFilterArray(object) {
  const urlFilter = [];
  Object.keys(object).forEach((key) => {
    let filter = null;
    let value = object[key].trim().replaceAll(", ", ",");
    if (value[value.length - 1] === ",") {
      value = value.slice(0, value.length - 1);
    }
    if (key.includes(".")) {
      if (value.includes(",")) {
        const indexFilters = value.split(",").map((val) => `${key}=q=${val}`);
        filter = `(${indexFilters})`;
      } else {
        filter = `${key}=q=${value}`;
      }
    } else {
      if (value.includes(",")) {
        filter = `${key}=in=(${value})`;
      } else {
        filter = `${key}==${value}`;
      }
    }
    urlFilter.push(filter);
  });
  return urlFilter;
}

// renameKey
// Rename a property in a dataset (i.e., array of objects)
//
// @param data an array of objects
// @param oldKey name of the property to rename
// @param newKey name to substitute
//
// @param an array of objects
export function renameKey(data, oldKey, newKey) {
  data.forEach(
    (row) => delete Object.assign(row, { [newKey]: row[oldKey] })[oldKey]
  );
}

// Remove Null Object Keys
// Remove all null keys from an object. This is the first step in preparing
// filters for a Molgenis DataExplorer URL
//
// @param data an object containing one or more Null keys
//
// @examples
// const filters = {
//   gender: 'female',
//   age: null,
//   country: 'Netherlands'
//   group: null
// }
// removeNullObjectKeys(filters)
// > { gender: 'female', country: 'Netherlands' }
//
// @return object
export function removeNullObjectKeys(data) {
  const filters = data;
  Object.keys(filters).forEach((key) => {
    if (filters[key] === null || filters[key] === "") {
      delete filters[key];
    }
  });
  return filters;
}

// setDataExplorerUrl
// Create full URL with filters for MOLGENIS (EMX1) DataExplorer
//
// @param entity EMX table location as <package>_<entity>
// @param array an array of filters (i.e., output of objectToUrlFilterArray)
//
// @examples
// const userInputs = {
//   gender: 'female',
//   age: null,
//   country: 'Australia, New Zealand'
//   group: null
// }
// const filters = removeNullObjectKeys(userInputs)
// const filterArray = objectToUrlFilterArray(filters)
// setDataExplorerUrl('database_table', filterArray)

export function setDataExplorerUrl(entity, array) {
  const filters = array.join(";");
  const filtersEncoded = encodeURIComponent(filters);
  const baseUrl = `/menu/plugins/dataexplorer?entity=${entity}&mod=data&hideselect=true`;
  const url = baseUrl + "&filter=" + filtersEncoded;
  return url;
}

// setSearchAllUrl
// Generate the Data Explorer URL for a search all query
//
// @param entity EMX table location as <package>_<entity>
// @param query a string containing a search term
//
// @examples
// const query = 'my-search-term'
//
// @return a string containing a URL to a dataexplorer table
export function setSearchAllUrl(entity, query) {
  const baseUrl = `/menu/plugins/dataexplorer?entity=${entity}&mod=data&hideselect=true`;
  const urlParamEncoded =
    "query%5Bq%5D%5B0%5D%5Boperator%5D=SEARCH&query%5Bq%5D%5B0%5D%5Bvalue%5D";
  const queryEncoded = encodeURIComponent(query);
  const url = `${baseUrl}&${urlParamEncoded}=${queryEncoded}`;
  return url;
}

// Sort Data
// Sort an dataset (array of objects) by a specific property
//
// @param data an array of objects
// @param column column to sort by
export function sortData(data, column) {
  return data.sort((current, next) => {
    return current[column] < next[column] ? -1 : 1;
  });
}

// Reverse Sort Data
// Reverse sort an dataset (array of objects) by a specific property
//
// @param data an array of objects
// @param column column to sort by
export function reverseSortData(data, column) {
  return data.sort((current, next) => {
    return current[column] < next[column] ? 1 : -1;
  });
}

// String as Number
// Attempt to parse string as a number
//
// @param value a string containing a number
// @return a number
export function stringAsNumber(value) {
  return typeof value === "string"
    ? parseFloat(value.replace(/,/g, ""))
    : value;
}

// subset data
// Select rows in dataset by value
//
// @param data an array of objects
// @param column column to search in
// @param value value to select rows by
//
// @return an array of objects
export function subsetData(data, column, value) {
  return data.filter((row) => row[column] === value);
}

// get today's date
export function today() {
  const date = new Date();
  return date.toLocaleDateString();
}

// daysdiff
// calculate the difference between two dates in days
//
// @param recent date object
// @param earliest date object
//
// @return integer
export function daysDiff(recent, earliest) {
  const diff = Math.abs(recent.getTime() - earliest.getTime());
  const daysdiff = Math.floor(diff / (1000 * 60 * 60 * 24));
  if (daysdiff === 0) {
    return "today";
  } else if (daysdiff === 1) {
    return "yesterday";
  } else {
    return `${daysdiff} days ago`;
  }
}

// windowReplaceUrl
// Open a URL in another tab
//
// @param url URL to open
//
export function windowReplaceUrl(url) {
  window.open(url, "_blank");
}

// validateNumRange
// Vue validator for floats. If false, the supplied value is not between 0 and 1
//
// @return boolean
export function validateNumRange(value) {
  return value >= 0 && value <= 1;
}
