package org.pm.patientservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.pm.patientservice.dto.PatientRequestDTO;
import org.pm.patientservice.dto.PatientResponseDTO;
import org.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import org.pm.patientservice.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // Endpoint to get all patients
    @GetMapping("")
    public ResponseEntity<List<PatientResponseDTO>> getPatients() {
        return ResponseEntity.ok().body(patientService.getAllPatients());
    }

    // Endpoint to create a new patient
    @PostMapping("")
    public ResponseEntity<PatientResponseDTO> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDTO patientRequest){
        PatientResponseDTO createdPatient = patientService.createPatient(patientRequest);
        return ResponseEntity.status(201).body(createdPatient);
    }

    @PutMapping("{id}")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable("id") UUID patientId, @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequest){
        PatientResponseDTO updatedPatient = patientService.updatePatient(patientId, patientRequest);
        return ResponseEntity.ok().body(updatedPatient);
    }


}
