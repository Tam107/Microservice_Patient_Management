package org.pm.patientservice.mapper;

import org.pm.patientservice.dto.PatientResponseDTO;
import org.pm.patientservice.model.Patient;

import java.util.UUID;

public class PatientMapper {

    public static PatientResponseDTO toDTO(Patient patient){

        return PatientResponseDTO.builder()
                .id(patient.getId().toString())
                .name(patient.getName())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .dateOfBirth(patient.getDateOfBirth().toString())
                .build();
    }


}
