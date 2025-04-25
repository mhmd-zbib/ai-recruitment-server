package com.zbib.hiresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Represents a physical address in the system
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "addresses",
    indexes = {
        @Index(name = "idx_address_city", columnList = "city"),
        @Index(name = "idx_address_country", columnList = "country"),
        @Index(name = "idx_address_postal_code", columnList = "postal_code"),
        @Index(name = "idx_address_location", columnList = "latitude, longitude")
    }
)
public class Address {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "street_address", length = 150)
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "formatted_address", length = 255)
    private String formattedAddress;
    
    // Latitude and longitude for mapping
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
} 