import { useSettingsStore } from "../stores/settingsStore";

export const mapToString = (object, property, prefix, suffix) => {
  if (!object) return "";

  prefix = prefix ? `${prefix} ` : "";
  suffix = suffix ? ` ${suffix}` : "";
  return object[property] ? `${prefix}${object[property]}${suffix}` : "";
};

export function mapObjArray(objects) {
  if (!objects) return [];
  if (!objects.some((o) => o.uri))
    return objects.map((item) => item.label || item.name);
  else
    return objects.map((item) => ({
      label: item.label || item.name,
      uri: item.uri || "#",
    }));
}

export function mapUrl(url) {
  url && (url.startsWith("http") ? url : "http://" + url);
}

export function mapRange(min, max, unit) {
  let range = "";
  if ((min || min === 0) && max) {
    range = `${min}-${max} `;
  } else if (min || min === 0) {
    range = `> ${min} `;
  } else if (max) {
    range = `< ${max} `;
  }
  if (range.length > 0 && unit.length) {
    range += unit.map((unit) => unit.label).join();
  } else {
    range = undefined;
  }
  return range;
}

function getObjectForValueExtraction(object, propertyKey) {
  /** this column is a nested propery */
  if (typeof propertyKey === "object") {
    const nextKey = Object.keys(propertyKey)[0];

    return getObjectForValueExtraction(object, nextKey);
  } else if (typeof object[propertyKey] === "object") {
    const keys = Object.keys(object[propertyKey]);
    let nextKey = "";

    /** find the next non-digit key in the array. */
    for (const key of keys) {
      if (isNaN(key)) {
        nextKey = key;
        break;
      }
    }
    /** found a pure array, return that */
    if (!nextKey) return object[propertyKey];

    return getObjectForValueExtraction(object[propertyKey], nextKey);
  } else {
    return object[propertyKey];
  }
}

/**
 *
 * @param {*} object collection / biobank
 * @param {*} columns column config
 * @returns an array of generator attributes: { label: columnInfo.label, type: columnInfo.type, value: attributeValue, component: columnInfo.component }
 */
export function getViewmodel(object, columns) {
  const attributes = [];

  for (const columnInfo of columns) {
    let attributeValue;
    let objectToExtractValueFrom = getObjectForValueExtraction(
      object,
      columnInfo.column
    );

    switch (columnInfo.type) {
      case "range": {
        const { min, max, unit } = columnInfo;
        attributeValue = mapRange(object[min], object[max], object[unit]) || "";
        break;
      }
      case "object": {
        attributeValue = mapToString(
          objectToExtractValueFrom,
          columnInfo.property,
          columnInfo.prefix,
          columnInfo.suffix
        );
        break;
      }
      case "custom": {
        attributeValue = objectToExtractValueFrom;
        break;
      }
      case "array": {
        attributeValue = objectToExtractValueFrom;
        break;
      }
      case "quality":
        attributeValue = object.quality;
        break;
      case "mref":
      case "categoricalmref": {
        attributeValue = mapObjArray(objectToExtractValueFrom);
        break;
      }
      default: {
        attributeValue = mapToString(
          object,
          columnInfo.column,
          columnInfo.prefix,
          columnInfo.suffix
        );
      }
    }
    /** component is only used as an override. undefined by default, the generator will handle this by type. */
    const attribute = {
      label: columnInfo.label,
      type: columnInfo.type,
      value: attributeValue,
      component: columnInfo.component,
    };

    if (columnInfo.showCopyIcon) {
      attribute.linkValue = columnInfo.copyValuePrefix
        ? `${columnInfo.copyValuePrefix}${attributeValue}`
        : attributeValue;
    }
    attributes.push(attribute);
  }

  return { attributes };
}

/**
 * Get all the types available within the collection tree
 */
function extractCollectionTypes(collections, prevCollectionHashmap) {
  let collectionTypes =
    prevCollectionHashmap && Object.keys(prevCollectionHashmap).length
      ? prevCollectionHashmap
      : {};

  for (const collection of collections) {
    if (collection.collectionType) {
      const foundTypes = collection.collectionType.map((type) => type.label);

      for (const type of foundTypes) {
        // use it as a hashmap
        if (!collectionTypes[type]) {
          collectionTypes[type] = "";
        }
      }
    }

    if (collection.sub_collections && collection.sub_collections.length) {
      const newHashmap = extractCollectionTypes(
        collection.sub_collections,
        collectionTypes
      );
      collectionTypes = { ...collectionTypes, ...newHashmap };
    }
  }
  return collectionTypes;
}

function mapSubcollections(collections, level) {
  const settingsStore = useSettingsStore();

  const subCollections = [];

  for (const collection of collections) {
    if (collection.sub_collections && collection.sub_collections.length) {
      const viewmodel = getViewmodel(
        collection,
        settingsStore.config.collectionColumns
      );
      viewmodel.sub_collections = mapSubcollections(
        collection.sub_collections,
        ++level
      );

      subCollections.push({
        level,
        ...collection,
        viewmodel,
      });
    } else {
      subCollections.push({
        level,
        ...collection,
        viewmodel: getViewmodel(
          collection,
          settingsStore.config.collectionColumns
        ),
      });
    }
  }
  return subCollections;
}

export function getCollectionDetails(collection) {
  const settingsStore = useSettingsStore();
  const viewmodel = getViewmodel(
    collection,
    settingsStore.config.collectionColumns
  );

  if (collection.sub_collections && collection.sub_collections.length) {
    viewmodel.sub_collections = mapSubcollections(
      collection.sub_collections,
      1
    );
  }

  return {
    ...collection,
    viewmodel,
  };
}

export const getBiobankDetails = (biobank) => {
  const settingsStore = useSettingsStore();

  if (biobank.collections && biobank.collections.length) {
    biobank.collections.collectionType = [];
    biobank.collections.collectionType = Object.keys(
      extractCollectionTypes(biobank.collections)
    );
    biobank.collectionDetails = [];

    const parentCollections = biobank.collections.filter(
      (collection) => !collection.parent_collection
    );

    for (const collection of parentCollections) {
      biobank.collectionDetails.push(getCollectionDetails(collection));
    }
  }

  return {
    ...biobank,
    viewmodel: getViewmodel(biobank, settingsStore.config.biobankColumns),
  };
};
