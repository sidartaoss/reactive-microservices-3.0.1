#!/usr/bin/env bash
echo "Building microservice JARs through the modules"
echo ""
cd ../
mvn install

echo ""

echo "Copying fat JARs to be Dockerized"

echo ""

echo "Copying project-008 fat JAR to project folder"
cd project-008
cp target/project-008-1.0-SNAPSHOT.jar .
echo "Ok"

echo ""

echo "Copying project-008 fat JAR to project folder"
cd ../project-009
cp target/project-009-1.0-SNAPSHOT.jar .
echo "Ok"

echo ""

echo "Deploying microservices to Kubernetes through the YAMLs"

echo ""

echo "Deploying the hello microservice application"
cd ../yaml
kubectl apply -f .


echo ""

echo "Well done!"