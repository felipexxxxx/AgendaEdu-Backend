package com.agendaedu.educacional.Services;

import com.agendaedu.educacional.DTOs.ActivityDTO;
import com.agendaedu.educacional.DTOs.ActivityResumoDTO;
import com.agendaedu.educacional.DTOs.StudentActivityDTO;
import com.agendaedu.educacional.Entities.*;
import com.agendaedu.educacional.Entities.enums.PresenceStatus;
import com.agendaedu.educacional.Entities.enums.Role;
import com.agendaedu.educacional.Repositories.ActivityRepository;
import com.agendaedu.educacional.Repositories.UserRepository;
import com.agendaedu.educacional.Repositories.PresenceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Collections;   
import org.springframework.security.core.Authentication;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final PresenceRepository presenceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService; 
   
    @Transactional
    public Activity criarAtividade(Activity activity) {
        Activity savedActivity = activityRepository.save(activity);

    // Pega todos os alunos da sala
        List<User> alunos = userRepository.findBySalaAndRole(activity.getSala(), Role.ALUNO);

    for (User aluno : alunos) {

        // Cria presença com status PENDENTE
        Presence presence = Presence.builder()
                .atividade(savedActivity)
                .usuario(aluno)
                .status(PresenceStatus.PENDENTE)
                .build();
        presenceRepository.save(presence);

        // Envia e-mail (opcional)
        String emailBody = """
            Olá %s,

            Uma nova atividade foi criada:

            Título: %s
            Descrição: %s
            Local: %s
            Data e Hora: %s

            Confirme sua presença no portal😊.

            Atenciosamente,
            ClassUP
            """.formatted(aluno.getNomeCompleto(), activity.getTitulo(), activity.getDescricao(), activity.getLocal(), activity.getDataHora());

        emailService.sendEmail(aluno.getEmail(), "Nova Atividade: " + activity.getTitulo(), emailBody);
    }

    return savedActivity;
}



    public List<ActivityDTO> getActivitiesBySala(Long salaId) {
        List<Activity> atividades = activityRepository.findBySalaId(salaId);
        return atividades.stream().map(a -> new ActivityDTO(
                a.getId(),
                a.getTitulo(),
                a.getDescricao(),
                a.getLocal(),
                a.getDataHora()
    )).toList();
}


    public Activity getById(Long id) {
        return activityRepository.findById(id).orElseThrow(() -> new RuntimeException("Atividade não encontrada"));
    }

    
    public List<StudentActivityDTO> listarAtividadesDoAluno() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User aluno = (User) auth.getPrincipal();
    
        if (!aluno.getRole().equals(Role.ALUNO)) {
            throw new RuntimeException("Apenas alunos podem acessar suas atividades.");
        }
    
        ClassEntity sala = aluno.getSala();
        if (sala == null) {
            return Collections.emptyList(); // Aluno ainda não está em uma sala
        }
    
        List<Activity> atividades = activityRepository.findBySalaId(sala.getId());
    
        return atividades.stream()
            .map(atividade -> {
                Presence presence = presenceRepository.findByUsuarioAndAtividade(aluno, atividade).orElse(null);
                PresenceStatus status = (presence != null) ? presence.getStatus() : PresenceStatus.PENDENTE;
    
                return new StudentActivityDTO(
                    atividade.getId(),
                    atividade.getTitulo(),
                    atividade.getDescricao(),
                    atividade.getDataHora(), // Certifique-se que é LocalDate
                    status,
                    atividade.getLocal()
                );
            })
            .toList();
    }
    @Transactional
    public void editarAtividade(Long id, ActivityDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User professor = (User) auth.getPrincipal();

        Activity atividade = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

        // Verifica se o professor atual é o dono da sala vinculada à atividade
        if (!atividade.getSala().getProfessor().getId().equals(professor.getId())) {
            throw new RuntimeException("Você não tem permissão para editar esta atividade.");
        }

        atividade.setTitulo(dto.titulo());
        atividade.setDescricao(dto.descricao());
        atividade.setLocal(dto.local());
        atividade.setDataHora(dto.data()); // aqui era o problema

        activityRepository.save(atividade);
    }

    @Transactional
    public void deletarAtividade(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User professor = (User) auth.getPrincipal();

        Activity atividade = activityRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));

    // Verifica se o professor atual é o dono da sala
        if (!atividade.getSala().getProfessor().getId().equals(professor.getId())) {
        throw new RuntimeException("Você não tem permissão para excluir esta atividade.");
        }

        activityRepository.delete(atividade);
}

public ActivityResumoDTO getResumoAtividade(Long atividadeId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User professor = (User) auth.getPrincipal();

    Activity atividade = activityRepository.findById(atividadeId)
        .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));


    if (atividade.getSala().getProfessor() == null || 
        !atividade.getSala().getProfessor().getId().equals(professor.getId())) {
        throw new RuntimeException("Você não tem permissão para visualizar essa atividade.");
    }

    int confirmados = presenceRepository.countByAtividadeAndStatus(atividade, PresenceStatus.CONFIRMADO);
    
    return new ActivityResumoDTO(
        atividade.getId(),
        atividade.getTitulo(),
        atividade.getDescricao(),
        atividade.getDataHora(),
        atividade.getLocal(),
        confirmados
    );
}




}
