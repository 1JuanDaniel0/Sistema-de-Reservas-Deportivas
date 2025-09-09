package com.example.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.bucket}")
    private String bucketName;
    @Value("${aws.region}")
    private String regionName;
    @Value("${aws.accessKeyId}")
    private String accessKey;
    @Value("${aws.secretAccessKey}")
    private String secretKey;

    private Region getRegion() {
        return Region.of(regionName);
    }

    private StaticCredentialsProvider getCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * Genera un nombre de archivo único y seguro para S3
     */
    private String generarNombreArchivoSeguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }

        // Obtener la extensión del archivo
        String extension = "";
        int lastDot = nombreOriginal.lastIndexOf('.');
        if (lastDot > 0) {
            extension = nombreOriginal.substring(lastDot);
        }

        // Generar nombre único: timestamp + UUID + extensión
        String nombreUnico = System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8) +
                extension;

        return nombreUnico;
    }

    /**
     * Construye la URL completa y correctamente encodificada para S3
     */
    private String construirUrlCompleta(String key) {
        try {
            // Dividir el key en partes para encodificar cada parte individualmente
            String[] partes = key.split("/");
            StringBuilder keyEncoded = new StringBuilder();

            for (int i = 0; i < partes.length; i++) {
                if (i > 0) {
                    keyEncoded.append("/");
                }
                // Encodificar cada parte para manejar espacios y caracteres especiales
                keyEncoded.append(URLEncoder.encode(partes[i], StandardCharsets.UTF_8));
            }

            return "https://" + bucketName + ".s3." + regionName + ".amazonaws.com/" + keyEncoded.toString();
        } catch (Exception e) {
            System.err.println("❌ Error al construir URL: " + e.getMessage());
            // Fallback: retornar URL básica
            return "https://" + bucketName + ".s3." + regionName + ".amazonaws.com/" + key;
        }
    }

    // SUBIR ARCHIVO PUBLICO - VERSIÓN MEJORADA
    public String subirArchivo(MultipartFile archivo, String carpetaDestino) throws IOException {
        System.out.println("🟡 [S3Service] Iniciando subida de archivo a S3...");
        System.out.println("📁 Bucket: " + bucketName);
        System.out.println("📂 Carpeta destino: " + carpetaDestino);
        System.out.println("📎 Nombre archivo original: " + archivo.getOriginalFilename());

        // Generar nombre de archivo seguro y único
        String nombreArchivoSeguro = generarNombreArchivoSeguro(archivo.getOriginalFilename());
        String key = carpetaDestino + "/" + nombreArchivoSeguro;

        System.out.println("🔄 Nombre archivo procesado: " + nombreArchivoSeguro);
        System.out.println("🗂️ Key completo: " + key);

        S3Client s3 = S3Client.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();

        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(archivo.getContentType())
                            .build(),
                    RequestBody.fromBytes(archivo.getBytes()));

            // Construir URL correctamente encodificada
            String urlCompleta = construirUrlCompleta(key);
            System.out.println("✅ Archivo público subido correctamente");
            System.out.println("🔗 URL final: " + urlCompleta);

            return urlCompleta;

        } catch (Exception e) {
            System.out.println("❌ ERROR al subir a S3: " + e.getMessage());
            throw e;
        }
    }

    public String generarUrlPreFirmada(String key, int minutos) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(regionName))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofMinutes(minutos))
                    .build();

            URL url = presigner.presignGetObject(presignRequest).url();
            return url.toString();
        }
    }

    public String subirArchivoPrivado(MultipartFile archivo, String carpetaDestino) throws IOException {
        // Generar nombre seguro para archivos privados también
        String nombreArchivoSeguro = generarNombreArchivoSeguro(archivo.getOriginalFilename());
        String key = carpetaDestino + "/" + nombreArchivoSeguro;

        S3Client s3 = S3Client.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();

        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(archivo.getContentType())
                        .build(),
                RequestBody.fromBytes(archivo.getBytes()));

        return key; // solo el key para archivos privados
    }
}