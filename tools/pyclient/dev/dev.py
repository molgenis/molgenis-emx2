#///////////////////////////////////////////////////////////////////////////////
# FILE: dev.py
# AUTHOR: David Ruvolo
# CREATED: 2023-05-22
# MODIFIED: 2023-07-20
# PURPOSE: development script for initial testing of the py-client
# STATUS: ongoing
# PACKAGES: **see below**
# COMMENTS: Designed to interact with the PetStore schema. Runs if you `cd` into
# the `src` directory.
#///////////////////////////////////////////////////////////////////////////////

from src.molgenis_emx2_pyclient.client import Client
import pandas as pd


# connect and sign in
db = Client('https://emx2.dev.molgenis.org/')
db.signin('admin','admin')

# get data
data = db.get(schema = '', table='') # run without specifying target
data = db.get(schema = 'pet store', table='') # run without specifying table
data = db.get(schema = 'pet store', table='Pet') # get Pets
data

data = db.get(schema = 'pet store', table='Pet', asDataFrame=True) # get Pets

#///////////////////////////////////////////////////////////////////////////////

# ~ 1 ~
# Check Import Methods

# ~ 1a ~
# Check import via the `data` parameters
# Add new record to the pet store with new tags

newTags = [
    {'name': 'brown', 'parent': 'colors'},
    {'name': 'canis', 'parent': 'species'},
]

newPets = [{
    'name': 'Woofie',
    'category': 'dog',
    'status': 'available',
    'weight': 6.8,
    'tags': 'brown,canis'
}]

# import new data
db.add(schema='pet store', table='Tag', data=newTags)
db.add(schema='pet store', table='Pet', data=newPets)


# retieve records
data = db.get(schema='pet store', table='Pet')
data

# drop records
tagsToRemove = [{'name': row['name']} for row in newTags if row['name'] == 'canis']
db.delete(schema='pet store', table='Pet', data=newPets)
db.delete(schema='pet store', table='Tag', data=tagsToRemove)

#///////////////////////////////////////

# ~ 1b ~
# Check import via the `file` parameter

# save datasets
pd.DataFrame(newTags).to_csv('dev/demodata/Tag.csv', index=False)
pd.DataFrame(newPets).to_csv('dev/demodata/Pet.csv', index=False)

# import files
db.add(schema='pet store', table='Tag', file='dev/demodata/Tag.csv')
db.add(schema='pet store', table='Pet', file='dev/demodata/Pet.csv')

db.delete(schema='pet store', table='Pet', file='dev/demodata/Pet.csv')
db.delete(schema='pet store', table='Tag', file='dev/demodata/Tag.csv')


# sign out
db.signout()