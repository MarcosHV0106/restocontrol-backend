package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.utp.RestoControl.Dto.LoteInsumoRequest;
import com.utp.RestoControl.Entity.AlertaInventario;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Repository.AlertaInventarioRepository;
import com.utp.RestoControl.Repository.RolRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback
class AlertaInventarioDatabaseTests {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private InsumoService insumoService;
    @Autowired
    private LoteInsumoService loteService;
    @Autowired
    private AlertaInventarioService alertaService;
    @Autowired
    private AlertaInventarioRepository alertaRepository;

    @Test
    void confirmaEstructuraMigracionYDatosHeredados() {
        assertEquals(1, contarTabla("lotes_insumos"));
        assertEquals(1, contarTabla("alertas_inventario"));
        assertTrue(rolRepository.existsByNombreRolIgnoreCaseAndEliminadoFalse("ALMACENERO"));

        String tipoStock = jdbcTemplate.queryForObject("""
                SELECT column_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'insumos'
                  AND column_name = 'stock_actual'
                """, String.class);
        assertEquals("decimal(12,4)", tipoStock.toLowerCase());

        Integer stockSinLote = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM insumos i
                WHERE i.eliminado = 0
                  AND COALESCE(i.stock_actual, 0) > 0
                  AND NOT EXISTS (
                      SELECT 1 FROM lotes_insumos l
                      WHERE l.id_insumo = i.id_insumo AND l.eliminado = 0
                  )
                """, Integer.class);
        assertEquals(0, stockSinLote);
    }

    @Test
    void generaAlertasRealesSinDuplicarlasYRevierteLosDatosDePrueba() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Insumo insumo = new Insumo();
        insumo.setNombreInsumo("PRUEBA-ALERTAS-" + sufijo);
        insumo.setDescripcion("Registro temporal de prueba de integración");
        insumo.setUnidadMedida("kg");
        insumo.setStockMinimo(BigDecimal.valueOf(5));
        insumo.setCostoUnitario(BigDecimal.ONE);
        insumo = insumoService.guardar(insumo);

        LoteInsumoRequest lote = new LoteInsumoRequest();
        lote.setCantidad(BigDecimal.valueOf(4));
        lote.setFechaVencimiento(LocalDate.now(ZONA_LIMA).plusDays(3));
        lote.setReferencia("PRUEBA-INTEGRACION");
        loteService.crear(insumo.getIdInsumo(), lote);

        alertaService.sincronizar();
        alertaService.sincronizar();

        Integer idInsumo = insumo.getIdInsumo();
        List<AlertaInventario> alertas = alertaRepository
                .findByEliminadoFalseOrderByFechaGeneracionDesc().stream()
                .filter(alerta -> alerta.getInsumo().getIdInsumo().equals(idInsumo))
                .filter(alerta -> alerta.getClaveActiva() != null)
                .toList();

        assertEquals(2, alertas.size());
        assertEquals(1, alertas.stream()
                .filter(alerta -> AlertaInventarioService.STOCK_BAJO.equals(alerta.getTipo())).count());
        assertEquals(1, alertas.stream()
                .filter(alerta -> AlertaInventarioService.VENCIMIENTO_PROXIMO.equals(alerta.getTipo())).count());
    }

    private Integer contarTabla(String tabla) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, tabla);
    }
}
