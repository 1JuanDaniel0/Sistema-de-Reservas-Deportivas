package com.example.project.repository;

import com.example.project.entity.ChatMensaje;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Integer> {

    List<ChatMensaje> findTop15ByUsuarioAndIdConversacionOrderByFechaDesc(Usuarios usuario, String idConversacion);
    // 🔧 FIX: Nuevo método para obtener historial reciente con contexto
    List<ChatMensaje> findTop10ByUsuarioAndIdConversacionOrderByFechaDesc(Usuarios usuario, String idConversacion);
}