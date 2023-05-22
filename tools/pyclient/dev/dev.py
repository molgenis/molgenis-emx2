#///////////////////////////////////////////////////////////////////////////////
# FILE: dev.py
# AUTHOR: David Ruvolo
# CREATED: 2023-05-22
# MODIFIED: 2023-05-22
# PURPOSE: development script for initial testing of the py-client
# STATUS: ongoing
# PACKAGES: **see below**
# COMMENTS: Designed to interact with the PetStore schema
#///////////////////////////////////////////////////////////////////////////////

from molgenis.client import Client
import csv

def to_csv(file, data, columns):
    with open(file, 'w', encoding='UTF-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        writer.writerows(data)
        f.close()


db = Client('https://david-emx2.molgeniscloud.org/')

# check sign in 
db.signin('','') # run with no credentials
db.signin('admin','') # run with only username
db.signin('admin','snazzy-pintail-woo-MOVER') # login

# get data
data = db.get(schema = '', table='') # run without specifying target
data = db.get(schema = 'pet store', table='') # run without specifying table
data = db.get(schema = 'pet store', table='Pet') # get Pets
data.text

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
data.text

# drop records
tagsToRemove = [{'name': row['name']} for row in newTags if row['name'] == 'canis']
db.delete(schema='pet store', table='Tag', data=tagsToRemove)
db.delete(schema='pet store', table='Pet', data=newPets)

#///////////////////////////////////////

# ~ 1b ~
# Check import via the `file` parameter

# save datasets
to_csv(
    file="dev/demodata/Tag.csv",
    data=newTags,
    columns=['name', 'parent']
)

to_csv(
    file="dev/demodata/Pet.csv",
    data=newPets,
    columns=list(newPets[0].keys())
)

# import files
db.add(schema='pet store', table='Tag', file='dev/demodata/Tag.csv')
db.add(schema='pet store', table='Pet', file='dev/demodata/Pet.csv')

db.delete(schema='pet store', table='Tag', file='dev/demodata/Tag.csv')
db.delete(schema='pet store', table='Pet', file='dev/demodata/Pet.csv')


#///////////////////////////////////////

# MISC

from molgenis.utils import toCsvString

w = toCsvString(data=tagsToRemove)
w.write()
w.csv

response = db.session.delete(
    url=f"{db.host}/pet store/api/csv/Tag",
    headers = {'Content-Type': 'text/csv'},
    data=w.csv
)
response.json()