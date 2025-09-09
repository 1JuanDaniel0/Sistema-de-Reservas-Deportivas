package com.example.project.repository.admin;

import com.example.project.entity.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LugarRepositoryAdmin extends JpaRepository<Lugar, Integer> {
}
