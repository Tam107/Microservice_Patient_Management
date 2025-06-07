package org.pm.patientservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientResponseDTO {

    private String id;

    private String name;

    private String email;

    private String address;

    private String dateOfBirth;
}
