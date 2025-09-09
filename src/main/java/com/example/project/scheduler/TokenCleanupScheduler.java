package com.example.project.scheduler;

import com.example.project.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final PasswordResetTokenRepository tokenRepository;

    @Scheduled(cron = "0 0 2 * * *") // Todos los días a las 2:00 AM
    @Transactional
    public void eliminarTokensExpirados() {
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        int eliminados = tokenRepository.deleteByFechaExpiracionBefore(ayer);
        System.out.println("🧹 Tokens expirados eliminados: " + eliminados);
    }

}
