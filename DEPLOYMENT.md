# Full Enterprise CI/CD Deployment Guide — ShopEasy

Stack: **GitHub → Jenkins → Maven → SonarQube → Nexus → Docker → Kubernetes (Terraform/EKS) → Prometheus/Grafana**

---

## Phase 1 — Push Code to GitHub

```bash
cd ecommerce
git init
git add .
git commit -m "Initial commit: ecommerce app with full CI/CD"
git branch -M main
git remote add origin https://github.com/your-username/ecommerce-app.git
git push -u origin main
```

Create a `.gitignore`:
```
target/
*.class
.idea/
*.iml
```

---

## Phase 2 — Provision Infrastructure with Terraform

This creates: VPC, EKS cluster (3 nodes), ECR repo, S3 bucket, and **one EC2 instance** that will host Jenkins + SonarQube + Nexus.

```bash
cd terraform
terraform init
terraform plan
terraform apply -auto-approve
```

Wait ~15-20 minutes for EKS to provision. Capture outputs:
```bash
terraform output
# devops_tools_public_ip = "x.x.x.x"
# cluster_endpoint = "..."
# ecr_repository_url = "..."
```

Configure kubectl:
```bash
aws eks update-kubeconfig --region ap-south-1 --name ecommerce-eks
kubectl get nodes
```

---

## Phase 3 — Set Up Jenkins, SonarQube, Nexus (on the EC2 instance)

SSH into the DevOps tools instance:
```bash
ssh ubuntu@<devops_tools_public_ip>
```

Copy `docker/docker-compose-tools.yml` to the server, then:
```bash
docker compose -f docker-compose-tools.yml up -d
```

This starts:
| Tool | URL | Default credentials |
|---|---|---|
| Jenkins | `http://<ip>:8080` | Get initial password: `docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword` |
| SonarQube | `http://<ip>:9000` | admin / admin (change on first login) |
| Nexus | `http://<ip>:8081` | admin / (check `docker exec nexus cat /nexus-data/admin.password`) |

### Configure Jenkins
1. Install suggested plugins + these additional ones:
   - Pipeline, Git, Docker Pipeline, Kubernetes CLI, SonarQube Scanner, Nexus Artifact Uploader, JaCoCo, Slack Notification
2. **Manage Jenkins → Tools**: Add Maven 3.9 (name it `Maven-3.9`), Add JDK 17 (name it `JDK-17`)
3. **Manage Jenkins → System**: Add SonarQube server (name it `SonarQube-Server`, URL `http://sonarqube:9000`, add a SonarQube token as `SONAR_AUTH_TOKEN` credential)
4. **Manage Jenkins → Credentials**, add:
   - `github-credentials` — GitHub personal access token
   - `docker-hub-credentials` — Docker Hub username/password
   - `kubeconfig-credentials` — paste contents of `~/.kube/config` (Secret file)
   - `SONAR_AUTH_TOKEN` — generated from SonarQube UI (My Account → Security → Generate Token)

### Configure SonarQube
1. Login → My Account → Security → Generate a token
2. Create a new project with key `ecommerce-app`
3. Paste the token into Jenkins as described above

### Configure Nexus
1. Login → create two **hosted Maven2** repositories: `maven-releases` and `maven-snapshots`
2. Update `pom.xml` `<distributionManagement>` URLs to point to `http://<devops-ip>:8081/repository/...`
3. Add Nexus credentials to your local `~/.m2/settings.xml` (and as Jenkins credential `nexus-credentials` if using the Nexus plugin)

---

## Phase 4 — Configure GitHub Webhook

1. GitHub repo → Settings → Webhooks → Add webhook
2. Payload URL: `http://<jenkins-ip>:8080/github-webhook/`
3. Content type: `application/json`
4. Trigger: Just the `push` event

---

## Phase 5 — Create the Jenkins Pipeline Job

1. Jenkins → New Item → **Pipeline**
2. Pipeline definition: **Pipeline script from SCM**
3. SCM: Git → your repo URL → credential: `github-credentials`
4. Script Path: `jenkins/Jenkinsfile`
5. Before running, update placeholders in `jenkins/Jenkinsfile`:
   - `DOCKER_REGISTRY` → your Docker Hub username (or Nexus Docker repo host)
   - GitHub repo URL in Stage 1
6. Save → **Build Now**

The pipeline will:
1. Checkout from GitHub
2. `mvn clean compile`
3. Run tests + JaCoCo coverage
4. SonarQube static analysis
5. Wait for Quality Gate pass
6. Package JAR + deploy to Nexus
7. Build Docker image
8. Push image to Docker Hub
9. Deploy to Kubernetes (apply all manifests in `k8s/`)
10. Smoke test the deployed service

---

## Phase 6 — Deploy Monitoring (Prometheus + Grafana)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

kubectl create namespace monitoring

helm install monitoring prometheus-community/kube-prometheus-stack \
  -f monitoring/prometheus-values.yaml \
  -n monitoring
```

Get Grafana URL:
```bash
kubectl get svc monitoring-grafana -n monitoring
```
Login: `admin / Admin@2024`

Import dashboards by ID in Grafana UI:
- `315` — Kubernetes Cluster Overview
- `6417` — Kubernetes Pods
- `4701` — JVM/Spring Boot metrics
- `7362` — MySQL Overview

---

## Phase 7 — Verify End-to-End

```bash
# Check app pods
kubectl get pods -n ecommerce

# Check service
kubectl get svc -n ecommerce

# Check ingress
kubectl get ingress -n ecommerce

# Check HPA
kubectl get hpa -n ecommerce

# View logs
kubectl logs -f deployment/ecommerce-app -n ecommerce

# Access app (via Ingress domain, or port-forward for quick test)
kubectl port-forward svc/ecommerce-service -n ecommerce 8080:80
curl http://localhost:8080/api/products
```

---

## Trigger a Full Pipeline Run

```bash
# Make a code change
git add .
git commit -m "feat: update product search"
git push origin main
```
GitHub webhook → Jenkins auto-triggers → full pipeline runs → new image deployed with zero downtime (RollingUpdate).

---

## Cleanup (Avoid Cloud Charges)

```bash
helm uninstall monitoring -n monitoring
kubectl delete namespace ecommerce
kubectl delete namespace monitoring

cd terraform
terraform destroy -auto-approve
```

---

## How to Explain This Pipeline in an Interview

> "I built a full enterprise CI/CD pipeline for a Java Spring Boot e-commerce app.
> Code is pushed to GitHub, which triggers Jenkins via webhook. Jenkins builds with
> Maven, runs unit tests with JaCoCo coverage, runs SonarQube static analysis with
> a quality gate that can fail the build, then deploys the artifact to Nexus and
> builds/pushes a Docker image. Finally it deploys to a Kubernetes cluster — provisioned
> via Terraform on AWS EKS — using rolling updates, HPA for auto-scaling, ConfigMaps/Secrets
> for configuration, and an Ingress for external access. Prometheus scrapes metrics from
> Spring Boot Actuator and Grafana visualizes them."

**Key talking points:**
- Quality gate as a pipeline gatekeeper (SonarQube)
- Artifact versioning via Nexus
- Multi-stage Docker build for small image size
- Zero-downtime deploys via Kubernetes RollingUpdate
- Infrastructure fully reproducible via Terraform (destroy/recreate anytime)
- Observability via Actuator + Prometheus + Grafana
