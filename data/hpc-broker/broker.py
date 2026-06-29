from molgenis_emx2_pyclient import Client

import json

server_url = "http://localhost:8080"

recipient_filter = {
    "filter": {
        "status": {
            "status": {
                "equals": ["COMPLETED"]
            }
        }
    }
}

msg_payload = {
    "recipientsFilter": json.dumps(recipient_filter),
    "subject": "Job completed",
    "body": "Job completed hello from python",
}

with Client(url=server_url) as client:
    client.signin(username='admin', password='admin')
    schemas = client.get_schemas()
    client.set_schema('hpcCatalogueTasks')
    jobs = client.get('Jobs', query_filter="status.status == COMPLETED")
    if len(jobs) == 0:
        print("No jobs found with status COMPLETED")
        exit(0)
    print(f"Found {len(jobs)} jobs with status COMPLETED")

    for job in jobs:
        job_id = job['id']
        print(job_id)
        # send email and update status to NOTIFIED
        msg_resp = client.session.post(
            url=f"{client.url}/hpcCatalogueTasks/api/message/",
            json=msg_payload
        )
        msg_code = msg_resp.status_code
        if msg_code == 200:
            print("Message sent successfully")
            update_resp = client.session.post(
                url=f"{client.url}/hpcCatalogueTasks/graphql",
                json= {
                    "query": "mutation update($value:[JobsInput]){update(Jobs:$value){message}}",
                    "variables": {
                        "value":[
                            {
                                "id": job['id'],
                                "status":{
                                    "status":"NOTIFIED",
                                },
                                "email": job['email'],
                            }
                        ]
                    }
                }
            )
            print(update_resp)
        else:
            print(f"Failed to send message, status code: {msg_code}")

