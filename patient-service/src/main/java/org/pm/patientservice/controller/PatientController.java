package org.pm.patientservice.controller;

import lombok.RequiredArgsConstructor;
import org.pm.patientservice.dto.PatientResponseDTO;
import org.pm.patientservice.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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


}
