package ar.gob.rdam.helptickets.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HelpTicketDTO {
    private Long id;
    private String email;
    private String mensaje;
    private LocalDateTime createdAt;
}
