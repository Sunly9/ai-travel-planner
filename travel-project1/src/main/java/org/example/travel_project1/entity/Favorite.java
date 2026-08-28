package org.example.travel_project1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "spotName"}), indexes = @Index(name = "idx_fav_user", columnList = "userId"))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String spotName;

    @Column
    private String spotData;

    @Column
    private LocalDateTime createTime;
}
