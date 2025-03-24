package com.agendaedu.educacional.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "notices") // ← garante que o nome da tabela seja "notice"
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String mensagem;

    @Column(name = "enviada_em")
    private LocalDateTime enviadaEm; // ← nome em camelCase para o Java

    @ManyToOne
    @JoinColumn(name = "sala_id") // ← coluna do banco que faz a FK com a sala
    @JsonIgnore // <-- Adicione isso
    private ClassEntity sala;
}
