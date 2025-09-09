package com.example.project.repository.admin;

import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Usuarios, Integer> {
}
