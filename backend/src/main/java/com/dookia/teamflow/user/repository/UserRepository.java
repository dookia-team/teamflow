package com.dookia.teamflow.user.repository;

import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(UserProvider provider, String providerId);

    Optional<User> findByEmail(String email);
}
