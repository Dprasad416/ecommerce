# terraform/main.tf
# Provisions: VPC, EKS Cluster, Node Group, ECR Repository, S3 (Terraform state, Nexus blobs)

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  backend "s3" {
    bucket = "ecommerce-tfstate-bucket"
    key    = "eks/terraform.tfstate"
    region = "ap-south-1"
  }
}

provider "aws" {
  region = var.aws_region
}

# ── VPC ──────────────────────────────────────────────────
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.0"

  name = "${var.project_name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["ap-south-1a", "ap-south-1b", "ap-south-1c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = true

  tags = {
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
  }

  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }
}

# ── EKS Cluster ──────────────────────────────────────────
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "19.21.0"

  cluster_name    = var.cluster_name
  cluster_version = "1.28"

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true

  eks_managed_node_groups = {
    main = {
      desired_size   = 3
      min_size       = 2
      max_size       = 6
      instance_types = ["t3.medium"]   # Bigger nodes — running app + MySQL + Jenkins agents etc.
      capacity_type  = "ON_DEMAND"

      labels = {
        Environment = "production"
        Project     = var.project_name
      }
    }
  }

  tags = {
    Project     = var.project_name
    Environment = "production"
    ManagedBy   = "Terraform"
  }
}

# ── ECR Repository (alternative/backup to Nexus Docker registry) ──
resource "aws_ecr_repository" "app" {
  name                 = "ecommerce-app"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# ── S3 bucket for Nexus blob storage (optional, if Nexus runs on EC2/K8s) ──
resource "aws_s3_bucket" "nexus_storage" {
  bucket = "${var.project_name}-nexus-storage"

  tags = {
    Purpose = "Nexus artifact storage"
  }
}

# ── EC2 instance for Jenkins, SonarQube, Nexus (single VM for cost saving) ──
resource "aws_instance" "devops_tools" {
  ami           = "ami-0f5ee92e2d63afc18"  # Ubuntu 22.04 in ap-south-1, verify before use
  instance_type = "t3.large"               # Needs decent RAM for Jenkins+SonarQube+Nexus
  subnet_id     = module.vpc.public_subnets[0]

  vpc_security_group_ids = [aws_security_group.devops_sg.id]

  root_block_device {
    volume_size = 50   # GB — SonarQube + Nexus + Jenkins workspaces need space
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project_name}-devops-tools"
  }

  user_data = <<-EOF
              #!/bin/bash
              apt-get update -y
              apt-get install -y docker.io openjdk-17-jdk
              systemctl enable docker
              systemctl start docker
              EOF
}

resource "aws_security_group" "devops_sg" {
  name_prefix = "${var.project_name}-devops-sg"
  vpc_id      = module.vpc.vpc_id

  # Jenkins
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  # SonarQube
  ingress {
    from_port   = 9000
    to_port     = 9000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  # Nexus
  ingress {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  # SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]   # Restrict to your IP in production!
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ── Outputs ───────────────────────────────────────────────
output "cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "devops_tools_public_ip" {
  value = aws_instance.devops_tools.public_ip
}

output "configure_kubectl" {
  value = "aws eks update-kubeconfig --region ${var.aws_region} --name ${var.cluster_name}"
}
