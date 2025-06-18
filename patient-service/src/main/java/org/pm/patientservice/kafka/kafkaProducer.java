package org.pm.patientservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.pm.patientservice.model.Patient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Slf4j
@Service
public class kafkaProducer {

    // how we define message type when sending messages to Kafka (message convert to byte[])
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    // auto-wired constructor for KafkaTemplate
    public kafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient) {
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        try{
            log.info("Sending Patient Event to Kafka Topic");
            kafkaTemplate.send("patient", event.toByteArray());
        }catch(Exception e){
            log.error("Error sending Patient Event to Kafka Topic {}", event);
        }
    }


}
