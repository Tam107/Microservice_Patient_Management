package org.pm.patientservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pm.patientservice.dto.validators.CreatePatientValidationGroup;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientRequestDTO {

    @NotBlank
    @Size(min = 3, max = 100, message = "Name cannot be blank and must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Address cannot be blank")
    private String address;

    @NotBlank(message = "Date of Birth cannot be blank")
    private String dateOfBirth;

    @NotBlank(groups = CreatePatientValidationGroup.class, message = "Registered is required")
    private String registeredDate;


}
