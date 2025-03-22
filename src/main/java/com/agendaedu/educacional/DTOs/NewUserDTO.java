package com.agendaedu.educacional.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewUserDTO {
    private String email;
    private String matricula;
    private String senha;
    private String mensagem;
}
