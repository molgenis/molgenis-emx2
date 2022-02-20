# check version
aws --version

# create an instance
INSTANCE_ID=$(aws ec2 run-instances --image-id ami-0ba724c59d3c2b346 --count 1 --instance-type t2.micro --key-name mswertz --security-group-ids sg-030fc72bb47b17f64 --subnet-id subnet-093e36ba6d1b5f420 --region eu-central-1 --instance-initiated-shutdown-behavior terminate --query 'Instances[0].InstanceId' --output text)
echo "starting AWS instance with id=${INSTANCE_ID}"

#wait
aws ec2 wait instance-status-ok
echo "starting complete"

sleep 5; #just to be sure

#get public DNS
PUBLIC_DNS=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].PublicIpAddress" --output=text )
echo "retrieved public IP=${PUBLIC_DNS}"

scp -o StrictHostKeyChecking=no build/libs/molgenis-emx2-8.49.2-feat-update-data-model-SNAPSHOT-all.jar ubuntu@$PUBLIC_DNS:molgenis-emx2.jar
echo "uploaded molgenis binary, starting emx2"

ssh -o StrictHostKeyChecking=no ubuntu@$PUBLIC_DNS 'nohup sudo java -DMOLGENIS_HTTP_PORT=80 -jar molgenis-emx2.jar 1>/dev/null 2>/dev/null &'
echo "emx2 started"

ssh -o StrictHostKeyChecking=no ubuntu@$PUBLIC_DNS 'nohup sudo shutdown -P +60 1>/dev/null 2>/dev/null &'
echo "sent shutdown timer set for 60 minutes"

#todo, post a message to github stating preview available until +60mins
