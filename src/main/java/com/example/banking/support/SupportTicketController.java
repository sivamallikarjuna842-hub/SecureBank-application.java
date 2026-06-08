package com.example.banking.support;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService ticketService;
    private final UserUtil userUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam(required = false) String priority) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(ticketService.createTicket(userId, subject, description, priority));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getMyTickets(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(ticketService.getUserTickets(userId));
    }

    @PutMapping("/{id}/respond")
    public ResponseEntity<ApiResponse<SupportTicket>> respond(@PathVariable Long id,
                                                               @RequestParam String response) {
        return ResponseEntity.ok(ticketService.respondToTicket(id, response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicket>> updateStatus(@PathVariable Long id,
                                                                    @RequestParam String status) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, status));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getAll() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }
}