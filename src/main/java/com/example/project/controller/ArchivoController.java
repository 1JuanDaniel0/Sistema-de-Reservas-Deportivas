package com.example.project.controller;

import com.example.project.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ArchivoController {

    @Autowired
    private S3Service s3Service;

    @GetMapping("/subir-archivo")
    public String mostrarFormulario() {
        return "subir-archivo";
    }

    @PostMapping("/subir-archivo")
    public String subirArchivo(@RequestParam("archivo") MultipartFile archivo, Model model) {
        try {
            String carpeta = "produccion";
            String key = s3Service.subirArchivo(archivo, carpeta);
            String url = s3Service.generarUrlPreFirmada(key, 10);

            model.addAttribute("url", url);
            return "archivo-subido";

        } catch (Exception e) {
            model.addAttribute("error", "Error al subir el archivo: " + e.getMessage());
            return "subir-archivo";
        }
    }
}
