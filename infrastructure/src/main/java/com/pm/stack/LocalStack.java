package com.pm.stack;

import com.amazonaws.services.ecs.model.Cluster;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

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
