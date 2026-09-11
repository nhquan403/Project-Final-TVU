package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.ReconcileRow;
import com.tvh.homestay.admin.dto.AdminDtos.ResolveRequest;
import com.tvh.homestay.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Hàng đợi đối soát thanh toán. */
@RestController
public class AdminPaymentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminPaymentService paymentsService;

    public AdminPaymentController(AdminPaymentService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @GetMapping("/api/admin/payments")
    public Page<ReconcileRow> queue(
            @RequestParam(required = false) String reconcileStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentsService.queue(reconcileStatus,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
    }

    @PostMapping("/api/admin/payments/{id}/resolve")
    public ReconcileRow resolve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolveRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return paymentsService.resolve(
                id, request == null ? null : request.note(), CurrentAdmin.idOf(principal));
    }

    @PostMapping("/api/admin/payments/{id}/confirm-manually")
    public ReconcileRow confirmManually(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolveRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return paymentsService.confirmManually(
                id, request == null ? null : request.note(), CurrentAdmin.idOf(principal));
    }
}
