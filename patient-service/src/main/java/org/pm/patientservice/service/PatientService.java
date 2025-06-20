package org.pm.patientservice.service;


import lombok.extern.slf4j.Slf4j;
import org.pm.patientservice.dto.PatientRequestDTO;
import org.pm.patientservice.dto.PatientResponseDTO;
import org.pm.patientservice.exception.EmailAlreadyExistsException;
import org.pm.patientservice.exception.PatientNotFoundException;
import org.pm.patientservice.grpc.BillingServiceGrpcClient;
import org.pm.patientservice.kafka.kafkaProducer;
import org.pm.patientservice.mapper.PatientMapper;
import org.pm.patientservice.model.Patient;
import org.pm.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final kafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, kafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getAllPatients(){
        List<Patient> patients = patientRepository.findAll();

        return patients.stream()
                .map(PatientMapper::toDTO)
                .toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequest){

        if (patientRepository.existsByEmail(patientRequest.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists: " + patientRequest.getEmail());
        }
        Patient patient = PatientMapper.toModel(patientRequest);
        Patient savedPatient = patientRepository.save(patient);
        billingServiceGrpcClient.createBillingAccount(patient.getId().toString(), patient.getName(), patient.getEmail());

        // Send event to Kafka topic after patient is saved
        kafkaProducer.sendEvent(savedPatient);
        log.info("Patient created and event sent to Kafka: {}", savedPatient);
        log.info("Check kafa topic for patient events {}", savedPatient.getId());

        return PatientMapper.toDTO(savedPatient);
    }

    public PatientResponseDTO updatePatient(UUID patientId, PatientRequestDTO patientRequest){

        Patient patient = patientRepository.findById(patientId).orElseThrow(
                ()->  new PatientNotFoundException("Patient not found with id: " + patientId)
        );

        if (patientRepository.existsByEmailAndIdNot(patientRequest.getEmail(), patientId) &&
            !patient.getEmail().equals(patientRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + patientRequest.getEmail());
        }

        patient.setName(patientRequest.getName());
        patient.setEmail(patientRequest.getEmail());
        patient.setAddress(patientRequest.getAddress());
        patient.setDateOfBirth(patientRequest.getDateOfBirth() != null ?
                LocalDate.parse(patientRequest.getDateOfBirth()) : patient.getDateOfBirth());
        patient.setRegisteredDate(patient.getRegisteredDate()!= null ?
                patient.getRegisteredDate() : LocalDate.now());
        Patient updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);
    }

    public void deletePatient(UUID patientId){
        if (!patientRepository.existsById(patientId)) {
            throw new PatientNotFoundException("Patient not found with id: " + patientId);
        }
        patientRepository.deleteById(patientId);
    }

}
