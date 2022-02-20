# check version
aws --version

# create an instance, that will shutdown after 60mins
INSTANCE_ID=$(aws ec2 run-instances --image-id ami-0ba724c59d3c2b346 --count 1 --instance-type t2.micro --key-name mswertz --security-group-ids sg-030fc72bb47b17f64 --subnet-id subnet-093e36ba6d1b5f420 --instance-initiated-shutdown-behavior terminate --user-data "nohup sudo shutdown -P +10 1>/dev/null 2>/dev/null &" --query 'Instances[0].InstanceId' --output text)
echo "starting AWS instance with id=${INSTANCE_ID}"

#wait
aws ec2 wait instance-status-ok --instance-ids $INSTANCE_ID
echo "starting complete"

#just wait a bit more
sleep 5;

#get public DNS
PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].PublicIpAddress" --output=text )
echo "retrieved public IP=${PUBLIC_IP}"

#get file name from build folder
JAR_FILE=$(ls build/libs/molgenis-emx2-*)

scp -o StrictHostKeyChecking=no $JAR_FILE ubuntu@$PUBLIC_IP:molgenis-emx2.jar
echo "uploaded molgenis binary, starting emx2"

ssh -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP 'nohup sudo java -DMOLGENIS_HTTP_PORT=80 -jar molgenis-emx2.jar 1>/dev/null 2>/dev/null &'
echo "emx2 started"

ssh -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP 'nohup sudo shutdown -P +10 1>/dev/null 2>/dev/null &'
echo "sent shutdown timer set for 10 minutes"

echo "::set-output name=result::$PUBLIC_IP"
