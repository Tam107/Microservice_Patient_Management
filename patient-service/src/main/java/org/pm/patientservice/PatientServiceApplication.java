package org.pm.patientservice;

import org.pm.patientservice.model.Patient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PatientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {

		Patient patient = new Patient();
		patient.setId(null); // ID will be generated automatically
		patient.setName("John Doe");
		patient.setEmail("hihi@gmail.com");
		patient.setAddress("Hanoi");
		patient.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
		patient.setRegisteredDate(java.time.LocalDate.now());


		return args -> {
			// This is where you can add any startup logic if needed
			System.out.println("Patient Service Application has started successfully.");
			System.out.println("Sample Patient: " + patient.toString());
		};
	}

}
