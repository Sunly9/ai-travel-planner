package org.example.travel_project1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "travel_plans", indexes = @Index(name = "idx_plan_user", columnList = "userId"))
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Integer days;

    @Column
    private Double budget;

    @Column(columnDefinition = "TEXT")
    private String planData;

    @Column
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;
}
