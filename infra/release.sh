set -e
#set -x

result=`cat /etc/nginx/sites-available/default | grep "proxy_pass http://localhost:808"`

liveSever=
otherServer=
newPort=
oldPort=
if [[ $result == *8080* ]]; then
  liveSever=/opt/server
  otherServer=/opt/server2
  newPort=8081
  oldPort=8080
else
  liveSever=/opt/server2
  otherServer=/opt/server
  newPort=8080
  oldPort=8081
fi

echo "Live server is $liveSever, other server is $otherServer"
echo "Starting new server on $newPort"

pid=`ps aux | grep java | grep "server.port=$newPort" | awk '{print $2}'`
if [ ! -z "$pid" ]; then
  echo "$otherServer is still running, stopping it"
  kill $pid
  sleep 5
fi


cp /tmp/financial-data-0.0.1-SNAPSHOT.jar "$otherServer/."


echo "Starting up server"

$otherServer/startup.sh &
#disown -a

echo "Waiting on startup"
sleep 5

i=0
for (( i = 0; i < 30; i++ )); do
  logResult=`tail -n 10 $otherServer/logs/appLog.log | grep -c "Started FinancialDataApplication" || true`
  if (( logResult > 0 )); then
    echo "Application started"
    break;
  fi
  echo "Waiting on startup $i"
  sleep 1;
done

if (( i > 28 )); then
    echo "Unable to start server";
    tail -n 50 $otherServer/logs/appLog.log;
    exit 1;
fi


tail -n 50 $otherServer/logs/appLog.log
echo "All started up, ready to switch traffic, waiting for 5s..."
sleep 5;

cp /etc/nginx/sites-available/default /tmp/default_backup
sed s/\{\{SERVER_PORT\}\}/$newPort/g "/opt/nginx/default" > /etc/nginx/sites-available/default

nginx -s reload

echo "Nginx restarted"

echo "Waiting for 30s, before killing old instance"
sleep 30

pid=`ps aux | grep java | grep "server.port=$oldPort" | awk '{print $2}'`

kill $pid

echo "All done, old server stopped"
disown -a
