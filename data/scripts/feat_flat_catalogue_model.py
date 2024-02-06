"""
Script to transform the DataCatalogue model to a flat, 'hierarchy-less' model
in which all staging area models live in harmony.

TODO: delete when pull request is ready for merging
"""

import pandas as pd


MODELS_DIR = '../_models/shared'


def transform_data_model():

    # Load DataCatalogue
    data_catalogue = pd.read_csv(f"{MODELS_DIR}/DataCatalogue-TODO.csv")
    print(data_catalogue.head())

    # Make a copy
    df = data_catalogue.copy()

    # Transform Organisations, Models, Networks and Studies into standalone tables that do not inherit from *Resources

    # Change tableName from 'Cohorts' to 'Resources' and add 'Cohorts' as profile

    # Save result to file
    df.to_csv(f"{MODELS_DIR}/DataCatalogue-FLAT.csv")


if __name__ == '__main__':
    transform_data_model()
