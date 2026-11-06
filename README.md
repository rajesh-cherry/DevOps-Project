# End-to-End DevOps Project: Spring Boot + Jenkins + ECR + EKS + Prometheus/Grafana

This repository is a hands-on learning project. It includes a Spring Boot REST API, Maven tests, Dockerfile, Jenkins pipeline, Kubernetes manifests, a kube-prometheus-stack values file, and a starter Terraform configuration.

## Architecture

GitHub -> Jenkins -> Maven build/tests -> Docker image -> Amazon ECR -> Amazon EKS -> Prometheus -> Grafana

## Prerequisites

- AWS account and AWS CLI configured
- Terraform 1.5+
- kubectl compatible with your EKS cluster
- Helm 3
- Jenkins agent with Java 21, Maven 3.9+, Docker, AWS CLI v2, and kubectl
- Jenkins agent IAM role/user permissions for ECR and EKS deployment
- GitHub repository containing this project

Never commit AWS access keys, kubeconfig files, passwords, or tokens.

## 1. Run application locally

```bash
cd application
mvn clean verify
mvn spring-boot:run
```

Try:
```bash
curl http://localhost:8080/api/employees
curl http://localhost:8080/api/employees/1
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

## 2. Build and run Docker locally

Run from repository root:
```bash
docker build -f docker/Dockerfile -t employee-api:local .
docker run --rm -p 8080:8080 employee-api:local
```

## 3. Create AWS infrastructure with Terraform

Review `terraform/main.tf` before applying. This starter uses a managed EKS node group with two t3.medium nodes and one NAT Gateway. AWS resources incur charges.

```bash
cd terraform
terraform init
terraform fmt -recursive
terraform validate
terraform plan
terraform apply
```

Record the ECR repository URL and cluster name from Terraform outputs.

The EKS module version and Kubernetes version are example pins. Before applying, check that the selected EKS Kubernetes version is currently supported in your AWS region and update `cluster_version` if necessary.

Configure kubectl:
```bash
aws eks update-kubeconfig --region us-east-2 --name devops-eks-cluster
kubectl get nodes
```

## 4. Create the ECR repository

Terraform creates `employee-api`. If you already have a repository with that name, import it into Terraform or change the repository name to avoid a name conflict.

## 5. Configure Jenkins

Create a Pipeline or Multibranch Pipeline job pointing to this GitHub repository. The Jenkins agent must have:
- `mvn`, `docker`, `aws`, and `kubectl` commands available
- Docker daemon access
- An IAM role (preferred) or securely managed AWS credentials
- ECR push permissions
- EKS cluster authentication and Kubernetes RBAC permissions to update the `devops` namespace

Set `EKS_CLUSTER` in the Jenkinsfile to your actual cluster name. Set `AWS_REGION` to your region. The pipeline deploys only when the branch is `main`.

Recommended ECR permissions for the Jenkins identity include `ecr:GetAuthorizationToken` and repository-scoped `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, and `ecr:PutImage`. The identity also needs `eks:DescribeCluster`. Kubernetes authorization is separate: grant the Jenkins IAM principal only the required permissions in the `devops` namespace.

## 6. Deploy once manually (after pushing an image)

Replace the placeholder image in `kubernetes/deployment.yaml` with your actual ECR image URI and tag.

```bash
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/hpa.yaml
kubectl -n devops get pods,svc,hpa
```

Test inside the cluster:
```bash
kubectl -n devops port-forward svc/employee-api 8080:80
curl http://localhost:8080/api/employees
```

## 7. Install Prometheus and Grafana

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack   --namespace monitoring --create-namespace   -f monitoring/kube-prometheus-stack-values.yaml
```

Set a strong Grafana password before installing. Then install the ServiceMonitor:
```bash
kubectl apply -f kubernetes/servicemonitor.yaml
```

Access Grafana locally:
```bash
kubectl -n monitoring port-forward svc/monitoring-grafana 3000:80
```
Open http://localhost:3000. Username is `admin`; use the password you configured. Prometheus is available through port-forwarding:
```bash
kubectl -n monitoring port-forward svc/monitoring-kube-prometheus-prometheus 9090:9090
```

The ServiceMonitor selector label `release: monitoring` must match the Helm release name. If you change the release name, update the label accordingly.

## 8. Useful troubleshooting commands

```bash
kubectl -n devops get all
kubectl -n devops describe pod POD_NAME
kubectl -n devops logs deployment/employee-api
kubectl -n devops rollout status deployment/employee-api
kubectl -n devops rollout history deployment/employee-api
kubectl -n devops rollout undo deployment/employee-api
kubectl get events -A --sort-by=.lastTimestamp
```

## Important notes / improvements for production

- The in-memory employee list resets when the application restarts; this is a demo API, not a production database-backed service.
- The Jenkinsfile assumes the Jenkins agent has Docker daemon access. For a hardened setup, use a dedicated build agent and consider rootless/buildkit-based image builds.
- Add SonarQube only after the basic pipeline works.
- Configure an AWS Load Balancer Controller and Ingress separately if you want public HTTP access. The included Service is ClusterIP.
- Configure TLS, secrets management, image vulnerability policies, resource quotas, network policies, and least-privilege IAM/RBAC before production use.
- Terraform destroy removes the EKS/VPC/ECR resources managed here. Review the plan carefully and preserve any images/data you need.
