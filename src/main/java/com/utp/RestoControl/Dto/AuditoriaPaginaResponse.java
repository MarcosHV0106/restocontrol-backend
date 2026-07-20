package com.utp.RestoControl.Dto;

import java.time.LocalDate;
import java.util.List;

public record AuditoriaPaginaResponse(
        LocalDate desde,
        LocalDate hasta,
        List<AuditoriaOperacionResponse> operaciones,
        long totalElementos,
        int totalPaginas,
        int pagina,
        int tamano,
        long exitosas,
        long fallidas
) {}
