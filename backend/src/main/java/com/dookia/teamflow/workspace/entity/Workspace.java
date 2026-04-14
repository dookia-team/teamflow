package com.dookia.teamflow.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * WORKSPACE 엔티티. ERD v0.1 §2 를 따른다.
 * slug 는 `slugify(name) + '-' + UUID 앞 8자` 형태로 전역 유일성을 보장한다.
 */
@Entity
@Table(name = "workspace")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createDate == null) {
            createDate = now;
        }
        if (updateDate == null) {
            updateDate = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updateDate = LocalDateTime.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public static Workspace create(String name) {
        return Workspace.builder()
            .name(name)
            .slug(generateSlug(name))
            .build();
    }

    private static String generateSlug(String name) {
        String base = name == null ? "" : name.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\p{IsHangul}]+", "-")
            .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "workspace";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
