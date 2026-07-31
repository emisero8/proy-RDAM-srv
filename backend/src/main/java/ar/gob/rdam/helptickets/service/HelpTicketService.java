package ar.gob.rdam.helptickets.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.HelpTicket;
import ar.gob.rdam.helptickets.dto.CrearTicketRequest;
import ar.gob.rdam.helptickets.dto.HelpTicketDTO;
import ar.gob.rdam.helptickets.repository.HelpTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpTicketService {

    private final HelpTicketRepository helpTicketRepository;

    @Transactional
    public HelpTicketDTO crear(String email, CrearTicketRequest request) {
        HelpTicket ticket = HelpTicket.builder()
                .email(email)
                .mensaje(request.getMensaje())
                .build();
        ticket = helpTicketRepository.save(ticket);
        return toDTO(ticket);
    }

    @Transactional(readOnly = true)
    public List<HelpTicketDTO> listarTodos() {
        return helpTicketRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public void eliminar(Long id) {
        if (!helpTicketRepository.existsById(id)) {
            throw new BusinessException("NOT_FOUND", "Ticket no encontrado.");
        }
        helpTicketRepository.deleteById(id);
    }

    private HelpTicketDTO toDTO(HelpTicket t) {
        return HelpTicketDTO.builder()
                .id(t.getId())
                .email(t.getEmail())
                .mensaje(t.getMensaje())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
