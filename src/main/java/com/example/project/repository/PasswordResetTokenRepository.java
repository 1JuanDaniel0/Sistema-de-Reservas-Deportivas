package com.example.project.repository;

import com.example.project.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.fechaExpiracion < :limite")
    int deleteByFechaExpiracionBefore(@Param("limite") LocalDateTime limite);

    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.correo = :correo")
    void deleteByCorreo(@Param("correo") String correo);

}
