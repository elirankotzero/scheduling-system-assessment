package com.scheduler.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parameter_schemas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"taskType", "parameterName"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    @Column(nullable = false)
    private String parameterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParameterType parameterType;

    @Column(nullable = false)
    private boolean required;

    private String description;

    private String defaultValue;
}
