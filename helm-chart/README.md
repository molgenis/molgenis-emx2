# helm chart

Cheatsheet

## prepare
first make sure you have ingress on your kubernetes cluster

second optionally override some params such as domain name if you want public dns

## basic operations

install on your kubernetes
```
helm install <name> <path to chart folder>
```

see it running and get its full name
```
kubectl get pods
```

see that it has ingressed and has address
```
kubectl get ingress
```

stop it again
```
helm delete <name>
```

For debug you might want to see ngingx logx.
First get name of the pod
```
kubectl get pods -n ingress-nginx
```
Then the logs
```
kubectl logs -n ingress-nginx <name of your nginx controller>
```

See your endpoint
```
kubectl get endpoints
```

FAQ

I used docker desktop and then I had to enable ingress

https://kubernetes.github.io/ingress-nginx/deploy/#docker-for-mac

