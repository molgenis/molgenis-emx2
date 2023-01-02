import { useSettingsStore } from "../stores/settingsStore"


export const mapToString = (object, property, prefix, suffix) => {
    if (!object) return ''

    prefix = prefix ? `${prefix} ` : ''
    suffix = suffix ? ` ${suffix}` : ''
    return object[property] ? `${prefix}${object[property]}${suffix}` : ''
}

export function mapObjArray (objects) {
    if (!objects) return []
    if (!objects.some(o => o.uri)) return objects.map(item => item.label || item.name)
    else return objects.map(item => ({ label: item.label || item.name, uri: item.uri || '#' }))
}

export function mapUrl (url) {
    url && (url.startsWith('http') ? url : 'http://' + url)
}

export function mapRange (min, max, unit) {
    let range = ''
    if ((min || min === 0) && max) {
        range = `${min}-${max} `
    } else if (min || min === 0) {
        range = `> ${min} `
    } else if (max) {
        range = `< ${max} `
    }
    if (range.length > 0 && unit.length) {
        range += unit.map(unit => unit.label).join()
    } else {
        range = undefined
    }
    return range
}

/**
 *
 * @param {*} object collection / biobank
 * @param {*} columns column config
 * @returns an array of generator attributes: { label: columnInfo.label, type: columnInfo.type, value: attributeValue, component: columnInfo.component }
 */
export function getViewmodel (object, columns) {
    const attributes = []

    for (const columnInfo of columns) {
        let attributeValue

        switch (columnInfo.type) {
            case 'range': {
                const { min, max, unit } = columnInfo
                attributeValue = mapRange(object[min], object[max], object[unit]) || ''
                break
            }
            case 'object': {
                attributeValue = mapToString(object[columnInfo.column], columnInfo.property, columnInfo.prefix, columnInfo.suffix)
                break
            }
            case 'custom': {
                attributeValue = object[columnInfo.column]
                break
            }
            case 'array': {
                attributeValue = object[columnInfo.column]
                break
            }
            case 'quality':
                attributeValue = object.quality
                break
            case 'mref':
            case 'categoricalmref': {
                attributeValue = mapObjArray(object[columnInfo.column])
                break
            }
            default: {
                attributeValue = mapToString(object, columnInfo.column, columnInfo.prefix, columnInfo.suffix)
            }
        }

        const attribute = { label: columnInfo.label, type: columnInfo.type, value: attributeValue, component: columnInfo.component }

        if (columnInfo.showCopyIcon) {
            attribute.linkValue = columnInfo.copyValuePrefix ? `${columnInfo.copyValuePrefix}${attributeValue}` : attributeValue
        }
        attributes.push(attribute)
    }

    return { attributes }
}

/**
 * Get all the types available within the collection tree
 */
function extractCollectionTypes (collections, prevCollectionHashmap) {
    let collectionTypes = prevCollectionHashmap && Object.keys(prevCollectionHashmap).length ? prevCollectionHashmap : {}

    for (const collection of collections) {
        if (collection.type) {
            const foundTypes = collection.type.map(type => type.label)

            for (const type of foundTypes) {
                // use it as a hashmap
                if (!collectionTypes[type]) {
                    collectionTypes[type] = ''
                }
            }
        }

        if (collection.sub_collections && collection.sub_collections.length) {
            const newHashmap = extractCollectionTypes(collection.sub_collections, collectionTypes)
            collectionTypes = { ...collectionTypes, ...newHashmap }
        }
    }
    return collectionTypes
}

function mapSubcollections (collections, level) {
    const settingsStore = useSettingsStore();

    const subCollections = []

    for (const collection of collections) {
        if (collection.sub_collections && collection.sub_collections.length) {
            const viewmodel = getViewmodel(collection, settingsStore.config.collectionColumns)
            viewmodel.sub_collections = mapSubcollections(collection.sub_collections, ++level)

            subCollections.push({
                level,
                ...collection,
                viewmodel
            })
        } else {
            subCollections.push({
                level,
                ...collection,
                viewmodel: getViewmodel(collection, settingsStore.config.collectionColumns)
            })
        }
    }
    return subCollections
}

export function getCollectionDetails(collection) {
    const settingsStore = useSettingsStore();
    const viewmodel = getViewmodel(collection, settingsStore.config.collectionColumns)

    if (collection.sub_collections && collection.sub_collections.length) {
        viewmodel.sub_collections = mapSubcollections(collection.sub_collections, 1)
    }

    return {
        ...collection,
        viewmodel
    }
}

export const getBiobankDetails = (biobank) => {
    const settingsStore = useSettingsStore();
    /* new Set makes a hashmap out of an array which makes every entry unique, then we convert it back to an array */
    biobank.collection_types = []

    if (biobank.collections.length) {
        biobank.collection_types = Object.keys(extractCollectionTypes(biobank.collections))
        biobank.collectionDetails = []

        const parentCollections = biobank.collections.filter(collection => !collection.parent_collection)

        for (const collection of parentCollections) {
            biobank.collectionDetails.push(getCollectionDetails(collection))
        }
    }

    return {
        ...biobank,
        viewmodel: getViewmodel(biobank, settingsStore.config.biobankColumns)
    }
}