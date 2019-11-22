#!/usr/bin/env bash
echo "Cleaning up target folder into the modules"
echo ""
cd ../
mvn clean

echo ""

echo "Cleaning up fat JARs"

echo ""

echo "Cleaning up fat JAR into project-008"
cd project-008
rm -rf project-008-1.0-SNAPSHOT.jar
echo "Ok"

echo ""

echo "Cleaning up fat JAR into project-008"
cd ../project-009
rm -rf project-009-1.0-SNAPSHOT.jar
echo "Ok"

echo ""

echo "Cleaning up current Kubernetes resources"

echo ""

echo "Cleaning up current Pods and Deployments"
kubectl delete deployment --all

echo ""

echo "Cleaning up current Ingresses"
kubectl delete ingress --all

echo ""

echo "Cleaning up current ConfigMaps"
kubectl delete configmap --all

echo ""

echo "Cleaning up current Services"
kubectl delete svc project008 project009

echo ""

echo "Well done!"