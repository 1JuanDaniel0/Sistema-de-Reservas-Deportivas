package com.example.project.repository;

import com.example.project.entity.IntentoIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntentoIpRepository extends JpaRepository<IntentoIp, Long> {
    Optional<IntentoIp> findByIpAndTipo(String ip, String tipo);
}
