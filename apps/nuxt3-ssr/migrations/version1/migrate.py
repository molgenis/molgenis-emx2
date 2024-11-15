from molgenis_emx2_pyclient import Client

username = 'admin'
password = 'admin'

# Initialize the client as a context manager
with Client(url='http://localhost:8080') as client:
    # Apply the 'signin' method with the username and password
    client.signin(username, password)
    
    # Perform other tasks
