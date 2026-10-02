package project.EnterpriseSaas.demo.modules.shift.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.common.enums.ShiftDepartment;
import project.EnterpriseSaas.demo.modules.shift.service.ShiftService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hotel-shifts")
@RequiredArgsConstructor
public class HotelShiftController {

    private final ShiftService srv;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveShift(
            @RequestHeader("x-tenant-id") UUID tId,
            @RequestHeader("x-branch-id") UUID bId) {
        return ResponseEntity.ok(ApiResponse.ok(srv.getActiveShift(bId, tId, ShiftDepartment.hotel)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listShifts(
            @RequestHeader("x-tenant-id") UUID tId,
            @RequestHeader("x-branch-id") UUID bId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(ApiResponse.ok(srv.listShifts(bId, tId, limit, offset, null, null, null, null, ShiftDepartment.hotel)));
    }

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<Object>> openShift(
            @RequestHeader("x-tenant-id") UUID tId,
            @RequestHeader("x-branch-id") UUID bId,
            @RequestBody Map<String, Object> body,
            Principal p) {
        UUID uId = UUID.fromString(p.getName());
        BigDecimal openCash = new BigDecimal(body.getOrDefault("openingCash", "0").toString());
        return ResponseEntity.ok(ApiResponse.ok(srv.openShift(bId, tId, uId, openCash, null, ShiftDepartment.hotel)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Map<String, Object>>> closeShift(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tId,
            @RequestBody Map<String, Object> body,
            Principal p) {
        UUID uId = UUID.fromString(p.getName());
        BigDecimal closeCash = new BigDecimal(body.getOrDefault("closingCash", "0").toString());
        String nts = (String) body.get("notes");
        return ResponseEntity.ok(ApiResponse.ok(srv.closeShift(id, tId, uId, closeCash, null, nts)));
    }
}