package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.PromotionRequest;
import com.tvh.homestay.admin.dto.AdminDtos.PromotionView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD mã khuyến mãi. */
@RestController
public class AdminPromotionController {

    private final AdminPromotionService promotions;

    public AdminPromotionController(AdminPromotionService promotions) {
        this.promotions = promotions;
    }

    @GetMapping("/api/admin/promotions")
    public List<PromotionView> list() {
        return promotions.list();
    }

    @PostMapping("/api/admin/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionView create(@Valid @RequestBody PromotionRequest request) {
        return promotions.create(request);
    }

    @PutMapping("/api/admin/promotions/{id}")
    public PromotionView update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return promotions.update(id, request);
    }

    @DeleteMapping("/api/admin/promotions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        promotions.delete(id);
    }
}
