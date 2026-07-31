package ar.gob.rdam.helptickets.repository;

import ar.gob.rdam.domain.entity.HelpTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpTicketRepository extends JpaRepository<HelpTicket, Long> {

    List<HelpTicket> findAllByOrderByCreatedAtDesc();
}
