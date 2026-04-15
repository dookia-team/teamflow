package com.dookia.teamflow.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TeamFlow USER 엔티티. ERD v0.1 §1 을 따른다.
 * OAuth 로그인과 자체 회원가입을 모두 지원하도록 provider / password_hash 가 모두 nullable.
 */
@Entity
@Table(
    name = "`user`",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_provider",
        columnNames = {"provider", "provider_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @Column(name = "user_id", unique = true, length = 100)
    private String userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;

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

    public void updateProfile(String name, String picture) {
        this.name = name;
        this.picture = picture;
    }

    public void softDelete() {
        this.deleteDate = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deleteDate != null;
    }

    public static User createFromOAuth(UserProvider provider, String providerId, String email, String name, String picture) {
        return User.builder()
            .email(email)
            .name(name)
            .picture(picture)
            .provider(provider)
            .providerId(providerId)
            .build();
    }
}
