import pandas as pd

<<<<<<< HEAD

=======
>>>>>>> 18e571442d0d65167c35aeafcabc0e71d7a83fcf
def spaces(s):
    new_s = ''
    i = 0

    # exceptions to the rule
    if not pd.isna(s):
        if s == 'Datasources':
            new_s = 'Data sources'
        elif s == 'Datasources.csv':
            new_s = 'Data sources.csv'
        elif s in ['DAPs', 'DAPs.csv']:
            new_s = s
        elif 'ETL' in s:
            if s == 'ETLstandardVocabularies':
                new_s = 'ETL standard vocabularies'
            elif s == 'ETLstandardVocabulariesOther':
                new_s = 'ETL standard vocabularies other'
        elif 'EU' in s:
            if s == 'accessNonEU':
                new_s = 'access non EU'
            elif s == 'accessNonEUConditions':
                new_s = 'access non EU conditions'
        elif s == 'ontologyTermURI':
            new_s = 'ontology term URI'
        elif s == 'pid':
            new_s = 'id'
    # get spaces and lowercase
        else:
            for x in s:
                if x.isupper() and not i == 0:
                    new_s += ' ' + x.lower()
                else:
                    new_s += x
                i += 1

    return new_s
