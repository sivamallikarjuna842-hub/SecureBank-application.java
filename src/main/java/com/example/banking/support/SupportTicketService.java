package com.example.banking.support;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.ReferenceNumberGenerator;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator refGen;

    public ApiResponse<SupportTicket> createTicket(Long userId, String subject, String description, String priority) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(refGen.generateTicketNumber())
                .subject(subject)
                .description(description)
                .priority(priority != null ? priority.toUpperCase() : "MEDIUM")
                .status("OPEN")
                .user(user)
                .build();

        ticketRepository.save(ticket);
        return ApiResponse.ok("Ticket created successfully", ticket);
    }

    public ApiResponse<List<SupportTicket>> getUserTickets(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(ticketRepository.findByUserOrderByCreatedAtDesc(user));
    }

    public ApiResponse<SupportTicket> respondToTicket(Long ticketId, String response) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAdminResponse(response);
        ticket.setStatus("RESOLVED");
        ticketRepository.save(ticket);
        return ApiResponse.ok("Ticket updated", ticket);
    }

    public ApiResponse<SupportTicket> updateTicketStatus(Long ticketId, String status) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status.toUpperCase());
        ticketRepository.save(ticket);
        return ApiResponse.ok("Ticket status updated", ticket);
    }

    public ApiResponse<List<SupportTicket>> getAllTickets() {
        return ApiResponse.ok(ticketRepository.findAll());
    }
}