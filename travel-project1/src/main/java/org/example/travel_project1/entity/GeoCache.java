package org.example.travel_project1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地理编码缓存：地名 + 城市 → 经纬度
 * 高德按调用次数计费，同一个景点反复解析没必要每次都打接口
 */
@Data
@Entity
@Table(name = "geo_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"address", "city"}),
        indexes = @Index(name = "idx_geo_addr", columnList = "address"))
public class GeoCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 查询用的地名（景点名或城市名） */
    @Column(nullable = false)
    private String address;

    /** 城市限定，用于让「故宫」优先解析到北京；没传则存空串（NULL 会让唯一约束失效） */
    @Column(nullable = false)
    private String city;

    private Double latitude;
    private Double longitude;

    /** 高德返回的标准地址 */
    private String formattedAddress;

    private String province;

    private LocalDateTime updatedAt;
}
