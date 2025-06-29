package com.pm.stack;


import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {

    /**
     * Represents the Virtual Private Cloud (VPC) resource used within the stack.
     * The VPC is created with default settings during stack initialization.
     *
     * This VPC is primarily utilized to provide network isolation and infrastructure
     * for resources like database instances and other services contained in the stack.
     */
    private final Vpc vpc;

    private final Cluster ecsCluster;

    private final String jwtScret = "VGhpcy1pcy1hLXNlY3VyZS1rZXktZm9yLXRlc3RpbmctSldULXByb2R1Y3Rpb24=";

    /**
     * Constructor for LocalStack.
     *
     * @param scope the scope in which this stack is defined
     * @param id    the ID of the stack
     * @param props the properties for the stack
     */
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();
        DatabaseInstance authServiceDb = createDatabase("AuthServiceDb", "auth_service_db");
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDb", "patient_service_db");

        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDbHealthCheck");

        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDbHealthCheck");

        CfnCluster mskCluster = createMskCluster();

        this.ecsCluster = createEcsCluster();

        FargateService authService = createFargateService(
                "AuthService",
                "auth-service",
                List.of(4005),
                authServiceDb,
                Map.of("JWT_SECRET", jwtScret)
        );

        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        FargateService billingService = createFargateService(
                "BillingService",
                "billing-service",
                List.of(4001, 9001), // 4001 for REST API, 9001 for gRPC
                null,
                null
        );

        FargateService analyticsService = createFargateService(
                "AnalyticsService",
                "analytics-service",
                List.of(4002, 9002), // 4002 for REST API, 9002 for gRPC
                null,
                null
        );

        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService = createFargateService(
                "PatientService",
                "patient-service",
                List.of(4000), // 4003 for REST API, 9003 for gRPC
                patientServiceDb,
                Map.of("BILLING_SERVICE_ADDRESS", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001")
        );
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);
    }


    // auth-service.patient-management.local
    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder() // set up a Cloud Map namespace for service discovery
                        .name("patient-management.local")
                        .build()).build();
    }

    /**
     * Creates and configures an AWS Fargate service with the specified parameters.
     *
     * This method sets up a Fargate task definition and service, including container definitions,
     * port mappings, logging configurations, and environment variables. It optionally integrates
     * with a database instance for data persistence and supports additional environment variable customization.
     *
     * @param id               the unique identifier for the Fargate service
     * @param imageName        the name of the container image to be used in the service
     * @param ports            a list of container ports to be exposed
     * @param db               the database instance to connect to (optional, can be null)
     * @param additionalEnvVars additional custom environment variables to be added to the container (optional, can be null)
     * @return the configured FargateService object representing the deployed service
     */
    private FargateService createFargateService(String id, String imageName, List<Integer> ports, DatabaseInstance db,
                                                Map<String, String> additionalEnvVars){
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, id+ "Task")
                .cpu(256) // CPU units for the task
                .memoryLimitMiB(512) // Memory limit in MiB
                .build();

        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName) // Prefix for the log stream
                                .build()));

        Map<String, String> envVars =new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510" +
                ", localhost.localstack.cloud:4511" +
                ",localhost.localstack.cloud:4512"); // replace with actual Kafka bootstrap servers

        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if (db != null){
            envVars.put("STRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName
            ));

            envVars.put("STRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("STRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName+"Container", containerOptions.build());
        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .build();
    }

    private Vpc createVpc() {
        // Create a VPC with default settings
        return Vpc.Builder.create(this, "PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2) // Limit to 2 Availability Zones for cost efficiency
                .build();
    }

    /**
     * Creates and configures a database instance within the stack.
     *
     * @param id     the unique identifier for the database instance
     * @param dbName the name of the database
     * @return a configured {@code DatabaseInstance} object representing the created database instance
     */
    private DatabaseInstance createDatabase(String id, String dbName ){
        return DatabaseInstance.Builder.create(this, id) // this is current stack context, id is unique identifier
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_17_2)
                .build())
                )
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO)) // cpu, store and memory configuration
                .allocatedStorage(20) // storage size in GB
                .credentials(Credentials.fromGeneratedSecret("admin_user")) // generates a secret for the admin user
                .databaseName(dbName) // name of the database
                .removalPolicy(RemovalPolicy.DESTROY) // policy to destroy the database when the stack is deleted
                .build();
    }

    // create health check for the database
    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){
        return CfnHealthCheck.Builder
                .create(this,id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30) // seconds
                        .failureThreshold(3) // number of failed checks before marking as unhealthy
                        .build())
                .build();
    }

    /**
     * Creates and configures an Amazon MSK (Managed Streaming for Apache Kafka) cluster within the stack.
     *
     * This method constructs an MSK cluster with the specified cluster name, Kafka version,
     * number of broker nodes, and broker node group configuration. The broker node group
     * includes details such as instance type, client subnets, and availability zone distribution.
     *
     * @return a configured {@code CfnCluster} object representing the created MSK cluster
     */
    private CfnCluster createMskCluster(){
        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets().stream().map(
                                ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT").build())
                .build();
    }

//    private createApiGatewayService(){
//
//    }
    
    /**
     * The main entry point for the application.
     * Initializes the application, defines stack properties, creates the stack,
     * and synthesizes the CloudFormation template.
     *
     * @param args command-line arguments passed to the program
     */
    public static void main(String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // convert code into a cloud formation template
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);
        app.synth();

        System.out.println("App synthesizing in progress...");
    }


}
