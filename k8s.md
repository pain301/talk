Monolithic applications => microservices

Linux Namespaces + Linux Control Groups
pid
user
uts
ipc
mnt
net

master(The Control Plane)
Kubernetes API Server
Scheduler
Controller Manager
etcd

worker
Kubelet
Kubernetes Service Proxy
Docker

docker run busybox echo "Hello world"

------

create app
app.js
```
const http = require('http');
const os = require('os');
console.log("lab app starting...");
var handler = function(request, response) {
  console.log("request from " + request.connection.remoteAddress);
  response.writeHead(200);
  response.end("You've hit " + os.hostname() + "\n");
};
var server = http.createServer(handler);
server.listen(8080);
```
Dockerfile
```
FROM node:7
ADD app.js /app.js
EXPOSE 8080
ENTRYPOINT ["node", "app.js"]
```
```
docker build -t apple .
docker run --name apple-c -p 8080:8080 -d apple
docker inspect apple-c
docker stop apple-c
docker rm apple-c
docker tag apple pain400/apple

docker login
docker push pain400/apple
```
```
kubectl run apple --image=pain400/apple --port=8080
```
1. Kubectl send a REST HTTP request to the Kubernetes API server
2. Create a new ReplicationController object
3. The ReplicationController create a new pod
4. Scheduler schedule the pod to one of the worker nodes
5. The Kubelet on that node instruct Docker to pull the specified image
6. Docker create and run the container

```
kubectl expose rc apple --type=LoadBalancer --name apple-http
```
```
kubectl scale rc apple --replicas=3
```

------

1. multiple containers or one container with multiple processes
Containers are designed to run only a single process per container (unless the process itself spawns child processes)

run multiple processes in container, keep all those processes running, restart if crash

2. pod: high-level construct bind containers together

3. organize apps into multiple pods, where each one contains only tightly related components or processes

splitting into multiple pods to improve the utilization of your infrastructure
splitting into multiple pods to enable individual scaling
put multiple containers into a single pod when app consists of one main process and one or more complementary processes

```
apiVersion: v1
kind: Pod
metadata:
  name: apple
spec:
  containers:
  - image: pain400/apple
    name: apple
    ports:
    - containerPort: 8080
      protocol: TCP
```
```
kubectl create -f apple-pod.yaml
kubectl logs apple-pod
kubectl logs apple-pod -c apple
```
```
kubectl port-forward apple-pod 8888:8080
```




minikube start --docker-env http_proxy=http://192.168.200.22:1087 \
               --docker-env https_proxy=http://192.168.200.22:1087 \
               --docker-env no_proxy=192.168.99.0/24

minikube start --docker-env HTTP_PROXY=http://192.168.200.22:1087 \
               --docker-env HTTPS_PROXY=https://192.168.200.22:1087 \
               --insecure-registry "10.0.0.0/24"

export http_proxy=http://0.0.0.0:1087;export https_proxy=http://0.0.0.0:1087;

minikube dashboard

kubectl get -f nginx.yaml -o yaml

```sh
kubectl get namespaces
kubectl create namespace dev
```

pods
```
apiVersion: v1
kind: Pod
metadata:
  name: nginx
  labels:
    env: test
spec:
  containers:
  - name: nginx
    image: nginx:1.7.9
    imagePullPolicy: IfNotPresent
    ports:
    - containerPort: 80
```

```sh
kubectl create -f nginx.yaml
kubectl get pods
kubectl get pods -o wide
kubectl get pods -n test
kubectl get pods -l env=test
kubectl get pods helloworld -o yaml

kubectl exec $POD_NAME env
kubectl exec -it $POD_NAME bash

kubectl describe pods -n test
kubectl delete pod nginx
kubectl delete pod -l env=test
kubectl logs $POD_NAME
```

```
apiVersion: v1
kind: Pod
metadata:
  name: redis
spec:
  containers:
  - name: redis
    image: redis
    volumeMounts:
    - name: redis-storage
      mountPath: /data/redis
  volumes:
  - name: redis-storage
    emptyDir: {}
```
```
apiVersion: v1
kind: Pod
metadata:
  name: redis
spec:
  containers:
  - name: redis
    image: redis
    volumeMounts:
    - name: redis-storage
      mountPath: /data/redis-i
  volumes:
  - name: redis-storage
    hostPath:
      path: /data/redis-o
      type: Directory
```

```
apiVersion: v1
kind: Pod
metadata:
  name: www
spec:
  containers:
  - name: nginx
    image: nginx
    volumeMounts:
    - mountPath: /srv/www
      name: www-data
      readOnly: true
  - name: git-monitor
    image: kubernetes/git-monitor
    env:
    - name: GIT_REPO
      value: http://github.com/some/repo.git
    volumeMounts:
    - mountPath: /data
      name: www-data
  volumes:
  - name: www-data
    emptyDir: {}
```

deployments
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  selector:
    matchLabels:
      app: nginx
  replicas: 2
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        ports:
        - containerPort: 80
```
```sh
kubectl create -f deploy.yaml --record
kubectl get deployments -n test
kubectl get deployments -l app=nginx
kubectl describe nginx-deployment
kubectl scale deployments nginx-deployment --replicas=5
kubectl autoscale deployments nginx-deployment --min=2 --max=3
kubectl apply -f deploy.yaml
kubectl apply -f update.yaml
kubectl delete deployment nginx-deployment
```
```sh
kubectl rollout history deployment/helloworld
kubectl rollout history deployment/helloworld --revision=1
```
```sh
kubectl rollout status deployment/helloworld
kubectl rollout undo deployment/helloworld --to-revision=2
```

services
```
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  ports:
  - port: 8000
    targetPort: 80
    protocol: TCP
  selector:
    app: nginx
```
```sh
kubectl create -f deploy.yaml
kubectl get services -n test
kubectl get services -l app=nginx
kubectl describe services nginx-service
kubectl delete service nginx-service
```

```
apiVersion: v1
kind: Pod
metadata:
  name: dapi-test-pod
spec:
  containers:
    - name: test-container
      image: k8s.gcr.io/busybox
      command: [ "/bin/sh", "-c", "env" ]
      envFrom:
      - configMapRef:
          name: special-config
  restartPolicy: Never
```
```sh
kubectl create configmap app-config --from-file=app.yaml
kubectl get configmaps
kubectl describe configmaps app-config
kubectl get configmaps app-config -o yaml
```

```
kubectl taint nodes node1 key=value:NoSchedule
kubectl taint nodes node1 key:NoSchedule-
```

```
kubectl label nodes node1 disktype=ssd
kubectl get nodes -L gpu
kubectl get nodes -l gpu=true

kubectl get nodes --show-labels

kubectl label pod hello app=foo --overwrite
kubectl delete pod -l gpu=true

kubectl logs mypod --previous

kubectl cluster-info
```

```
kubectl delete rc helloworld --cascade=false
```

```
kubectl get endpoints
```

```
openssl genrsa -out tls.key 2048
openssl req -new -x509 -key tls.key -out tls.cert -days 360 -subj /CN=kns.lab.com
kubectl create secret tls tls-secret --cert=tls.cert --key=tls.key

kubectl create secret generic generic-secert --from-file=tls.key --from-file=tls.cert --from-file=foo
```

configmap
```
kubectl create configmap nginx-config --from-file=config/
```

kubectl port-forward helloworld-6df947dff7-cgq6k 8080:80

kubectl create secret docker-registry docker-secret \
  --docker-username=pain400 \
  --docker-email=pagepain400@gmail.com \
  --docker-password=


kubectl proxy
curl 127.0.0.1:8001