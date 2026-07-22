package dev.vuemind.api.beneficiaries;

import dev.vuemind.api.beneficiaries.dto.BeneficiariesResponse;
import dev.vuemind.api.beneficiaries.dto.BeneficiaryDto;
import dev.vuemind.api.beneficiaries.dto.CreateBeneficiaryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiariesController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiariesController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public BeneficiariesResponse list() {
        return new BeneficiariesResponse(beneficiaryService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryDto create(@RequestBody CreateBeneficiaryRequest request) {
        return beneficiaryService.create(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        beneficiaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
