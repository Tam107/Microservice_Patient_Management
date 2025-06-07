package org.pm.patientservice.service;


import lombok.RequiredArgsConstructor;
import org.pm.patientservice.dto.PatientResponseDTO;
import org.pm.patientservice.mapper.PatientMapper;
import org.pm.patientservice.model.Patient;
import org.pm.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public List<PatientResponseDTO> getAllPatients(){
        List<Patient> patients = patientRepository.findAll();

        return patients.stream()
                .map(PatientMapper::toDTO)
                .toList();
    }

}
