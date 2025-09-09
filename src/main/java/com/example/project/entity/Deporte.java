package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "deporte")
public class Deporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDeporte", nullable = false)
    private Integer idDeporte;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deporte deporte = (Deporte) o;
        return idDeporte != null && idDeporte.equals(deporte.idDeporte);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
