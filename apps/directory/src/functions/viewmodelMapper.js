import { useSettingsStore } from "../stores/settingsStore";
import { sortCollectionsByName } from "./sorting";

export const getName = (contact) => {
  const { title_before_name, first_name, last_name, title_after_name } =
    contact;

  let name = "";

  if (title_before_name) name += `${title_before_name} `;
  if (first_name) name += `${first_name} `;
  if (last_name) name += `${last_name} `;
  if (title_after_name) name += ` ${title_after_name}`;

  return name !== "" ? name.trim() : undefined;
};

export const mapToString = (object, property, prefix, suffix) => {
  if (!object) return "";

  if (typeof object === "string") return object;

  prefix = prefix ? `${prefix} ` : "";
  suffix = suffix ? ` ${suffix}` : "";
  return object[property] ? `${prefix}${object[property]}${suffix}` : "";
};

function getUriIfAvailable(item) {
  if (item.uri) return "uri";
  if (item.url) return "uri";
  if (item.ontologyTermURI) return "ontologyTermURI";

  return "";
}

export function mapObjArray(objects) {
  if (!objects) return [];
  if (!objects.some((o) => getUriIfAvailable(o)))
    return objects.map((item) => item.label || item.name);
  else
    return objects.map((item) => ({
      label: item.label || item.name,
      uri: item[getUriIfAvailable(item)],
    }));
}

export function mapUrl(url) {
  return url ? (url.startsWith("http") ? url : "http://" + url) : url;
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
  if (range.length > 0 && unit?.label) {
    range += unit.label;
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
    if (collection.type) {
      const foundTypes = collection.type.map((type) => type.label);

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
  const sortedCollections = sortCollectionsByName(collections);

  const sub_collections = [];

  for (const collection of sortedCollections) {
    if (collection.sub_collections && collection.sub_collections.length) {
      const viewmodel = getViewmodel(
        collection,
        settingsStore.config.collectionColumns
      );
      viewmodel.sub_collections = mapSubcollections(
        collection.sub_collections,
        ++level
      );

      sub_collections.push({
        level,
        ...collection,
        viewmodel,
      });
    } else {
      sub_collections.push({
        level,
        ...collection,
        viewmodel: getViewmodel(
          collection,
          settingsStore.config.collectionColumns
        ),
      });
    }
  }
  return sub_collections;
}

export function getCollectionDetails(collection) {
  const settingsStore = useSettingsStore();
  const viewmodel = getViewmodel(
    collection,
    settingsStore.config.collectionColumns
  );

  if (collection.sub_collections?.length) {
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

  if (biobank.collections?.length) {
    biobank.collections.type = [];
    biobank.collections.type = Object.keys(
      extractCollectionTypes(biobank.collections)
    );
    biobank.collectionDetails = [];

    const parentCollections = biobank.collections.filter(
      (collection) => !collection.parent_collection
    );

    const sortedParentCollections = sortCollectionsByName(parentCollections);

    for (const collection of sortedParentCollections) {
      biobank.collectionDetails.push(getCollectionDetails(collection));
    }
  }

  return {
    ...biobank,
    viewmodel: getViewmodel(biobank, settingsStore.config.biobankColumns),
  };
};

export const collectionReportInformation = (collection) => {
  const collectionReport = {};

  collectionReport.head = getNameOfHead(collection.head) || undefined;

  if (collection.contact) {
    collectionReport.contact = {
      name: getName(collection.contact),
      email: collection.contact.email ? collection.contact.email : undefined,
      phone: collection.contact.phone ? collection.contact.phone : undefined,
    };
  }

  if (collection.also_known) {
    collectionReport.also_known = collection.also_known
      ? mapAlsoKnownIn(collection)
      : undefined;
  }

  if (collection.biobank) {
    collectionReport.biobank = {
      id: collection.biobank.id,
      name: collection.biobank.name,
      juridical_person: collection.biobank.juridical_person,
      country: collection.country.label || collection.country.name,
      report: `/biobank/${collection.biobank.id}`,
      website: collection.biobank.url,
      email: collection.biobank.contact
        ? collection.biobank.contact.email
        : undefined,
    };
  }

  if (collection.network) {
    collectionReport.networks = collection.network.map((network) => {
      return {
        name: network.name,
        report: `/network/${network.id}`,
      };
    });
  }

  collectionReport.certifications = mapQualityStandards(collection.quality);

  collectionReport.collaboration = [];

  if (collection.collaboration_commercial) {
    collectionReport.collaboration.push({ name: "Commercial", value: "yes" });
  }
  if (collection.collaboration_non_for_profit) {
    collectionReport.collaboration.push({
      name: "Not for profit",
      value: "yes",
    });
  }

  // Give this information to the report, so we can use it in the breadcrumb.
  if (collection.parent_collection) {
    collectionReport.parentCollection = collection.parent_collection;
  }

  return collectionReport;
};

export const mapNetworkInfo = (data) => {
  return data.network.map((network) => {
    return {
      name: { value: network.name, type: "string" },
      report: { value: `/network/${network.id}`, type: "report" },
    };
  });
};

export const getNameOfHead = (head) => {
  if (!head) return "";

  const { first_name, last_name, role } = head;

  let name = "";

  if (first_name) name += `${first_name} `;
  if (last_name) name += `${last_name} `;
  if (role) name += `(${role})`;

  return name !== "" ? name.trim() : undefined;
};

export const mapHeadInfo = (instance) => {
  if (instance.head) {
    return {
      name: {
        value: getName(instance.head),
        type: "string",
      },
      website: { value: mapUrl(instance.head.url), type: "url" },
      email: {
        value: instance.head.email,
        type: "email",
      },
      country: {
        value: instance.head.country
          ? instance.head.country.label || instance.head.country.name
          : undefined,
        type: "string",
      },
    };
  } else {
    return {};
  }
};

export const mapContactInfo = (instance) => {
  if (instance.contact) {
    return {
      name: {
        value: getName(instance.contact),
        type: "string",
      },
      website: { value: mapUrl(instance.contact.url), type: "url" },
      email: {
        value: instance.contact.email,
        type: "email",
      },
      juridical_person: { value: instance.juridical_person, type: "string" },
      country: {
        value: instance.contact.country
          ? instance.contact.country.label || instance.contact.country.name
          : undefined,
        type: "string",
      },
    };
  } else {
    return {};
  }
};

export const mapAlsoKnownIn = (instance) => {
  let arr = [];

  if (instance.also_known) {
    for (const item of instance.also_known) {
      arr.push({ value: item.url, type: "url", label: item.name_system });
    }
  }

  return arr;
};

export const mapQualityStandards = (instance) => {
  let arr = [];

  if (instance) {
    for (const quality of instance) {
      arr.push(quality.quality_standard.label);
    }
  }

  return arr;
};
