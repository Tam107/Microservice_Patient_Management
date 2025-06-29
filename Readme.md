

# Patient Management System with Microservices: Java Spring Boot & AWS

This project is a production-ready **Patient Management System** built using a **microservices architecture** with **Java Spring Boot**, containerized with **Docker**, and deployable on **AWS**. It incorporates various modern technologies and best practices, including **gRPC**, **Kafka**, **API Gateway**, **Spring Security**, and **CloudFormation** for infrastructure-as-code. The system supports patient CRUD operations, billing, analytics, authentication, and more, with a focus on scalability, security, and fault tolerance.

## Table of Contents
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Technologies Used](#technologies-used)
- [Features](#features)
- [Project Structure](#project-structure)
- [Setup and Installation](#setup-and-installation)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [AWS Infrastructure Setup](#aws-infrastructure-setup)
- [Deployment on AWS](#deployment-on-aws)
- [API Documentation](#api-documentation)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Project Overview
This project demonstrates the development and deployment of a patient management system using a microservices architecture. It includes services for managing patient data, billing, analytics, and authentication, all orchestrated with Docker and deployed on AWS using infrastructure-as-code principles. The project follows a tutorial-style progression, as outlined in the chapter timestamps, covering everything from project setup to production deployment.

## Architecture
The system is built using a microservices architecture, with the following key components:
- **Patient Service**: Handles CRUD operations for patient data, communicates with a database, and integrates with gRPC and Kafka.
- **Billing Service**: Manages billing operations, implemented using gRPC for inter-service communication.
- **Analytics Service**: Consumes patient events via Kafka for real-time analytics.
- **Auth Service**: Handles user authentication and JWT-based token validation using Spring Security.
- **API Gateway**: Routes requests to appropriate services and integrates with the auth service for secure access.
- **Kafka Broker**: Facilitates event-driven communication between services.
- **Database**: Each service uses its own database (e.g., PostgreSQL) for data persistence.
- **AWS Infrastructure**: Includes a VPC, ECS clusters, MSK (Kafka), RDS databases, and a load-balanced application gateway.

## Technologies Used
- **Java Spring Boot**: Core framework for building microservices.
- **Docker**: Containerization of services.
- **gRPC**: High-performance inter-service communication.
- **Kafka**: Event-driven messaging for analytics.
- **Spring Security**: Authentication and authorization with JWT.
- **OpenAPI**: API documentation.
- **PostgreSQL**: Relational database for persistent storage.
- **AWS**: Cloud infrastructure (VPC, ECS, MSK, RDS, ALB, etc.).
- **CloudFormation**: Infrastructure-as-code for AWS deployment.
- **LocalStack**: Local AWS environment for testing.
- **JUnit & Testcontainers**: Integration testing.
- **Maven**: Dependency management and build tool.

## Features
- **Patient Management**: Create, read, update, and delete patient records with validation and error handling.
- **Billing Integration**: gRPC-based billing service for patient-related transactions.
- **Real-Time Analytics**: Kafka-based analytics service for processing patient events.
- **Secure Authentication**: JWT-based authentication with login and token validation endpoints.
- **API Gateway**: Centralized routing and security enforcement.
- **OpenAPI Documentation**: Auto-generated API docs for all services.
- **Containerization**: Dockerized services for consistent deployment.
- **AWS Deployment**: Scalable infrastructure with ECS, MSK, RDS, and a load-balanced application gateway.
- **Integration Testing**: Comprehensive tests for login, patient operations, and more.
- **Error Handling**: Custom exceptions and validation for robust APIs.

## Project Structure
The project is organized into multiple microservices, each with its own codebase and Dockerfile:
```
patient-management-system/
├── patient-service/                # Patient CRUD operations, gRPC client, Kafka producer
├── billing-service/                # gRPC-based billing service
├── analytics-service/              # Kafka consumer for analytics
├── auth-service/                   # Authentication and JWT token management
├── api-gateway/                    # Routes requests and enforces security
├── infrastructure/                 # CloudFormation templates for AWS
├── docker-compose.yml              # Orchestrates services locally
├── README.md                       # Project documentation
```

## Setup and Installation
### Prerequisites
- Java 17 or later
- Maven
- Docker and Docker Compose
- AWS CLI (for deployment)
- LocalStack (for local AWS testing)
- PostgreSQL (optional for local DB setup)

### Steps
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-repo/patient-management-system.git
   cd patient-management-system
   ```

2. **Set Up Environment**:
    - Configure environment variables for each service (e.g., database URLs, Kafka broker, etc.) in `.env` or `application.yml` files.
    - Ensure Docker and LocalStack are running.

3. **Build the Project**:
   ```bash
   mvn clean install
   ```

4. **Run Locally with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

## Running the Application
1. **Start Services**:
    - Use `docker-compose up` to start all services, including Kafka, databases, and the API gateway.
    - Services will be available at their respective ports (e.g., API Gateway at `http://localhost:8080`).

2. **Access API Documentation**:
    - OpenAPI docs are available at `http://localhost:8080/swagger-ui.html` (via API Gateway).

3. **Test Endpoints**:
    - Use tools like Postman or cURL to test endpoints (e.g., `/patients`, `/auth/login`).

## Testing
The project includes integration tests using JUnit and Testcontainers:
1. **Run Tests**:
   ```bash
   mvn test
   ```
2. **Test Coverage**:
    - Tests cover login, patient CRUD operations, and unauthorized access scenarios.

## AWS Infrastructure Setup
The AWS infrastructure is defined using CloudFormation templates in the `infrastructure/` directory. The following components are created:

1. **VPC**:
    - A Virtual Private Cloud (VPC) is created to provide an isolated network environment for the application.
    - Includes public and private subnets across multiple availability zones for high availability.

2. **Databases (RDS)**:
    - PostgreSQL RDS instances are set up for the patient and auth services.
    - Configured with security groups and subnet groups for secure access.

3. **Database Health Check**:
    - Health check mechanisms ensure database availability and connectivity.
    - Monitors RDS instances for uptime and performance.

4. **MSK Cluster**:
    - An Amazon Managed Streaming for Apache Kafka (MSK) cluster is created to handle event-driven messaging.
    - Configured for high throughput and fault tolerance.

5. **ECS Cluster**:
    - An Amazon Elastic Container Service (ECS) cluster is set up to run Dockerized microservices.
    - Uses Fargate for serverless container management.

6. **ECS Services**:
    - Individual ECS services are defined for each microservice (patient, billing, analytics, auth, and API gateway).
    - Configured with task definitions, scaling policies, and service discovery.

7. **Load-Balanced Application Gateway**:
    - An Application Load Balancer (ALB) is created to distribute traffic to the API gateway.
    - Configured with listener rules and target groups for routing requests.

## Deployment on AWS
1. **Set Up AWS CLI**:
    - Configure AWS credentials using `aws configure`.

2. **Deploy Infrastructure**:
    - Navigate to the `infrastructure/` directory.
    - Deploy CloudFormation stacks:
      ```bash
      aws cloudformation deploy --stack-name patient-system --template-file stack.yml
      ```

3. **Push Docker Images**:
    - Build and push images to Amazon ECR:
      ```bash
      docker build -t patient-service .
      aws ecr get-login-password | docker login --username AWS --password-stdin <ecr-uri>
      docker tag patient-service <ecr-uri>/patient-service:latest
      docker push <ecr-uri>/patient-service:latest
      ```

4. **Test Deployment**:
    - Access the load-balanced API Gateway URL provided by CloudFormation outputs.

## API Documentation
- **OpenAPI**: Auto-generated docs for all services, accessible via the API Gateway.
- **Endpoints**:
    - `POST /auth/login`: Authenticate users and return JWT.
    - `GET /patients`: Retrieve patient list (requires JWT).
    - `POST /patients`: Create a new patient.
    - `PUT /patients/{id}`: Update patient details.
    - `DELETE /patients/{id}`: Delete a patient.

## Troubleshooting
- **Database Connection Issues**: Verify database URLs and credentials in `application.yml`.
- **Kafka Errors**: Ensure the MSK cluster is running and accessible.
- **AWS Deployment Failures**: Check CloudFormation stack events for errors.
- **gRPC Issues**: Confirm proto files are correctly generated and services are running.
- **ALB Issues**: Verify listener rules and target group health checks.

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/your-feature`).
3. Commit changes (`git commit -m "Add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a pull request.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

