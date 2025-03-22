package com.agendaedu.educacional.Services;

import com.agendaedu.educacional.Entities.ClassEntity;
import com.agendaedu.educacional.Entities.Role;
import com.agendaedu.educacional.Entities.User;
import com.agendaedu.educacional.Entities.ClassHistoryEntity;
import com.agendaedu.educacional.Repositories.ClassRepository;
import com.agendaedu.educacional.Repositories.UserRepository;
import com.agendaedu.educacional.Repositories.ClassHistoryRepository;
import java.util.List;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ClassHistoryRepository salaHistoricoRepository;

    @Transactional
    public ClassEntity createClass(ClassEntity classEntity) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User user = (User) auth.getPrincipal();

    if (!user.getRole().equals(Role.PROFESSOR)) {
        throw new RuntimeException("Apenas professores podem criar salas.");
    }

    boolean salaExiste = classRepository.existsByNomeAndProfessor(classEntity.getNome(), user);
    if (salaExiste) {
        throw new RuntimeException("Você já criou uma sala com esse nome.");
    }

    String codigo = "SALA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    classEntity.setProfessor(user);
    classEntity.setCodigoAcesso(codigo);
    return classRepository.save(classEntity);
}

    @Transactional
    public String joinClass(String codigoDeEntrada) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        ClassEntity classEntity = classRepository.findByCodigoAcesso(codigoDeEntrada)
                .orElseThrow(() -> new RuntimeException("Código de sala inválido"));

        if (user.getSala() != null && user.getSala().getId().equals(classEntity.getId())) {
        throw new RuntimeException("Você já está nessa sala.");
        }

        if (user.getSala() != null) {
        throw new RuntimeException("Você já está em uma sala. Só é permitido ingressar em uma.");
        }

        user.setSala(classEntity);
        userRepository.save(user);

        return "Usuário adicionado à sala com sucesso.";
    }
    @Transactional
    public String encerrarSemestre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User professor = (User) auth.getPrincipal();

    if (!professor.getRole().equals(Role.PROFESSOR)) {
        throw new RuntimeException("Apenas professores podem encerrar o semestre.");
    }

    // Busca apenas salas onde ele ainda é o professor vinculado
    List<ClassEntity> salasAtivas = classRepository.findByProfessor(professor);

    if (salasAtivas.isEmpty()) {
        throw new RuntimeException("Você não possui salas ativas para encerrar.");
    }

    for (ClassEntity sala : salasAtivas) {
        // Se a sala já estiver no histórico, a gente não ignora — a gente verifica se AINDA está ativo!
        // Isso garante que professor só será desvinculado se ainda está vinculado
        if (sala.getProfessor() != null && sala.getProfessor().getId().equals(professor.getId())) {

            // 1. Registra professor no histórico
            if (!salaHistoricoRepository.existsBySalaAndUsuarioAndRole(sala, professor, Role.PROFESSOR)) {
                salaHistoricoRepository.save(ClassHistoryEntity.builder()
                        .usuario(professor)
                        .sala(sala)
                        .role(Role.PROFESSOR)
                        .dataEncerramento(LocalDateTime.now())
                        .build());
            }

            // 2. Registra alunos e desvincula
            List<User> alunos = userRepository.findBySalaAndRole(sala, Role.ALUNO);
            for (User aluno : alunos) {
                if (!salaHistoricoRepository.existsBySalaAndUsuarioAndRole(sala, aluno, Role.ALUNO)) {
                    salaHistoricoRepository.save(ClassHistoryEntity.builder()
                            .usuario(aluno)
                            .sala(sala)
                            .role(Role.ALUNO)
                            .dataEncerramento(LocalDateTime.now())
                            .build());
                }

                aluno.setSala(null);
            }

            // 3. Desvincula o professor da sala
            sala.setProfessor(null);

            // 4. Salva atualizações
            userRepository.saveAll(alunos);
            classRepository.save(sala);
        }
    }

    return "Semestre encerrado com sucesso. Todas as salas foram encerradas.";
}




    public List<ClassHistoryEntity> listarHistoricoUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

    return salaHistoricoRepository.findByUsuario(user);
}

    @Transactional
    public String removerAlunoDaSala(Long alunoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User professor = (User) auth.getPrincipal();

    // Valida se é professor
    if (!professor.getRole().equals(Role.PROFESSOR)) {
        throw new RuntimeException("Apenas professores podem remover alunos.");
    }

    // Busca o aluno
    User aluno = userRepository.findById(alunoId)
        .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

    // Valida se o aluno está em uma sala
    if (aluno.getSala() == null) {
        throw new RuntimeException("Este aluno não está em nenhuma sala.");
    }

    // Verifica se o professor é o dono da sala
    if (!aluno.getSala().getProfessor().getId().equals(professor.getId())) {
        throw new RuntimeException("Você não é o professor responsável por esta sala.");
    }

    // Remove aluno da sala
    aluno.setSala(null);
    userRepository.save(aluno);

    return "Aluno removido da sala com sucesso.";
}



   

}
