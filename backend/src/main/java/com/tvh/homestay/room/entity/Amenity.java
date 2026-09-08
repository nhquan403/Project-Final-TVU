package com.tvh.homestay.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "amenities")
@Getter
@Setter
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmenityCategory category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}
