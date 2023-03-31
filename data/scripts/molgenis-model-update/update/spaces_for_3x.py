import pandas as pd
from update.update_3_x import float_to_int
import os


def spaces(s, database):
    new_s = ''
    i = 0

    # exceptions to the rule
    if not pd.isna(s):
        if s == 'Datasources':
            new_s = 'Data sources'
        elif s == 'datasources':
            new_s = 'data sources'
        elif s == 'Datasources.csv':
            new_s = 'Data sources.csv'
        elif s in ['DAPs', 'DAPs.csv']:
            new_s = s
        # TODO: add Areas of information for Data sources (ds)
        elif s == 'AreasOfInformation.csv':
            new_s = 'Areas of information cohorts.csv'
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
        elif s == 'ontologyTermURI' and not database == 'CatalogueOntologies':
            new_s = 'ontology term URI'
        elif s == 'ontologyTermURI' and database == 'CatalogueOntologies':
            new_s = s
        elif s == 'pid':
            new_s = 'id'
        elif s in ['mg_insertedBy', 'mg_insertedOn', 'mg_updatedBy','mg_updatedOn']:
            new_s = s
        # get spaces and lowercase
        else:
            for x in s:
                if x.isupper() and not i == 0:
                    new_s += ' ' + x.lower()
                else:
                    new_s += x
                i += 1

    return new_s


def get_new_column_names(df, database):
    """Remove spaces from column names
    """
    old_names = df.columns.to_list()
    new_names = []

    for name in old_names:
        new_name = spaces(name, database)
        new_names.append(new_name)
    df.columns = new_names

    return df


class Spaces:
    """Get spaces in column names and table names
    """
    def __init__(self, database):
        self.database = database
        self.path = './files/' + self.database + '_data/'

    def get_spaces(self):
        """Get spaces and lowercase in table names and column names
        """
        for file_name in os.listdir(self.path):
            if not file_name == '_files':
                if file_name == 'InstitutionTypes.csv':
                    new_file_name = 'Organisation types.csv'
                else:
                    df = pd.read_csv(self.path + file_name, keep_default_na=False)
                    df = get_new_column_names(df, self.database)
                    df = float_to_int(df)  # convert float back to integer
                    df.to_csv(self.path + file_name, index=False)
                    new_file_name = spaces(file_name, self.database)
                os.rename(os.path.join(self.path, file_name), os.path.join(self.path, new_file_name))

