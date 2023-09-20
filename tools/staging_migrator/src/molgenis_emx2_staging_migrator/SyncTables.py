class TablesToSync:
    """The dictionary values are set to None or in case of UMCG cohorts to GDPR."""
    COHORT_STAGING_TO_DATA_CATALOGUE_ZIP = {
        'Variable mappings': None,
        'Variable values': None,
        'Repeated variables': None,
        'Variables': None,
        'Dataset mappings': None,
        'Datasets': None,
        'Documentation': None,
        'Publications': None,
        'Collection events': None,
        'Subcohort counts': None,
        'Subcohorts': None,
        'External identifiers': None,
        'Cohorts': None,
        'Data resources': None,
        'Resources': None,
        'Extended resources': None,
        'Contacts': None
    }

    UMCG_COHORT_STAGING_TO_DATA_CATALOGUE_ZIP = {
        'Documentation': None,
        'Publications': None,
        'Collection events': None,
        'Subcohort counts': None,
        'Subcohorts': None,
        'External identifiers': None,
        'Cohorts': None,
        'Data resources': None,
        'Resources': None,
        'Extended resources': None,
        'Contacts': 'GDPR'
    }


class TablesToDelete:
    COHORT_STAGING_TO_DATA_CATALOGUE_ZIP = {
        'VariableMappings': 'mappings',
        'DatasetMappings': 'mappings',
        'VariableValues': 'variables',
        'RepeatedVariables': 'variables',
        'Variables': 'variables',
        'Datasets': 'variables',
        'CollectionEvents': 'resource',
        'Documentation': 'resource',
        'Contacts': 'resource',
        'SubcohortCounts': 'subcohort',
        'Subcohorts': 'resource',
        'ExternalIdentifiers': 'resource',
        # 'ExtendedResources': 'id',
        'DataResources': 'id',
        # 'Publications': None, # doi filter not inplemented
        'Cohorts': 'id'
    }
    UMCG_COHORT_STAGING_TO_DATA_CATALOGUE_ZIP = {
        'Documentation': 'resource',
        'Contacts': 'resource',
        'CollectionEvents': 'resource',
        'SubcohortCounts': 'subcohort',
        'Subcohorts': 'resource',
        'ExternalIdentifiers': 'resource',
        'Cohorts': 'id'
    }
