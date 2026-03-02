package com.ola.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ola.constants.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Vehicle {
    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(length = 50)
    private String id;
    @Column(length = 15)
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(length = 50)
    private String owner;
    @Column(length = 50)
    @NotBlank(message = AppConstants.MODEL_MANDATORY)
    private String model;
    @Column(length = 50)
    @NotBlank(message = AppConstants.BRAND_MANDATORY)
    private String brand;
    @Column(length = 50)
    @JsonProperty("licence_number")
    @NotBlank(message = AppConstants.LICENCE_PLATE_NUMBER_MANDATORY)
    private String licencePlateNumber; //AA-AB-1234
    @JsonProperty("association_date")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(length = 30)
    private Date AssociationDate;
}
