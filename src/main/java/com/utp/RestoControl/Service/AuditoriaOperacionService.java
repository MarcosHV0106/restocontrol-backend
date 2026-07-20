package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.AuditoriaOperacionResponse;
import com.utp.RestoControl.Dto.AuditoriaOpcionesResponse;
import com.utp.RestoControl.Dto.AuditoriaPaginaResponse;
import com.utp.RestoControl.Dto.AuditoriaRegistro;
import com.utp.RestoControl.Entity.AuditoriaOperacion;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.AuditoriaOperacionRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaOperacionService {

    private static final int TAMANO_MAXIMO = 100;
    private static final int DIAS_MAXIMOS = 366;

    private final AuditoriaOperacionRepository repository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarOperacion(AuditoriaRegistro registro) {
        AuditoriaOperacion operacion = new AuditoriaOperacion();
        operacion.setFechaHora(LocalDateTime.now());
        operacion.setIdUsuario(registro.idUsuario());
        operacion.setCorreoUsuario(valorSeguro(registro.correoUsuario(), "sistema"));
        operacion.setNombreUsuario(resolverNombreUsuario(registro));
        operacion.setRolUsuario(valorSeguro(registro.rolUsuario(), "SISTEMA"));
        operacion.setModulo(valorSeguro(registro.modulo(), "GENERAL"));
        operacion.setAccion(valorSeguro(registro.accion(), "ACTUALIZAR"));
        operacion.setMetodoHttp(valorSeguro(registro.metodoHttp(), "N/A"));
        operacion.setRuta(valorSeguro(registro.ruta(), "/"));
        operacion.setRecursoId(recortar(registro.recursoId(), 80));
        operacion.setResultado(valorSeguro(registro.resultado(), "FALLO"));
        operacion.setEstadoHttp(registro.estadoHttp());
        operacion.setDuracionMs(Math.max(0, registro.duracionMs()));
        operacion.setDireccionIp(recortar(registro.direccionIp(), 64));
        operacion.setRequestId(recortar(registro.requestId(), 64));
        operacion.setDetalle(recortar(registro.detalle(), 500));
        operacion.setTipoError(recortar(registro.tipoError(), 120));
        repository.save(operacion);
    }

    @Transactional(readOnly = true)
    public AuditoriaPaginaResponse consultar(
            LocalDate desde,
            LocalDate hasta,
            Integer idUsuario,
            String modulo,
            String accion,
            String resultado,
            String texto,
            int pagina,
            int tamano
    ) {
        validarRango(desde, hasta);
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.min(TAMANO_MAXIMO, Math.max(1, tamano));
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        Specification<AuditoriaOperacion> filtros = crearFiltros(
                inicio, finExclusivo, idUsuario, modulo, accion, resultado, texto);
        Pageable pageable = PageRequest.of(
                paginaSegura,
                tamanoSeguro,
                Sort.by(Sort.Direction.DESC, "fechaHora", "idAuditoria")
        );
        Page<AuditoriaOperacion> paginaResultado = repository.findAll(filtros, pageable);
        long exitosas = repository.count(filtros.and(resultadoIgual("EXITO")));
        long fallidas = repository.count(filtros.and(resultadoIgual("FALLO")));

        return new AuditoriaPaginaResponse(
                desde,
                hasta,
                paginaResultado.getContent().stream().map(AuditoriaOperacionResponse::from).toList(),
                paginaResultado.getTotalElements(),
                paginaResultado.getTotalPages(),
                paginaResultado.getNumber(),
                paginaResultado.getSize(),
                exitosas,
                fallidas
        );
    }

    @Transactional(readOnly = true)
    public AuditoriaOpcionesResponse obtenerOpciones() {
        List<AuditoriaOpcionesResponse.UsuarioAuditoria> usuarios = repository.buscarUsuarios().stream()
                .map(fila -> new AuditoriaOpcionesResponse.UsuarioAuditoria(
                        (Integer) fila[0],
                        String.valueOf(fila[1]),
                        String.valueOf(fila[2]),
                        String.valueOf(fila[3])
                ))
                .toList();
        return new AuditoriaOpcionesResponse(repository.buscarModulos(), repository.buscarAcciones(), usuarios);
    }

    private Specification<AuditoriaOperacion> crearFiltros(
            LocalDateTime inicio,
            LocalDateTime finExclusivo,
            Integer idUsuario,
            String modulo,
            String accion,
            String resultado,
            String texto
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> condiciones = new ArrayList<>();
            condiciones.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaHora"), inicio));
            condiciones.add(criteriaBuilder.lessThan(root.get("fechaHora"), finExclusivo));

            if (idUsuario != null) {
                condiciones.add(criteriaBuilder.equal(root.get("idUsuario"), idUsuario));
            }
            agregarIgualSiExiste(condiciones, criteriaBuilder, root.get("modulo"), modulo);
            agregarIgualSiExiste(condiciones, criteriaBuilder, root.get("accion"), accion);
            agregarIgualSiExiste(condiciones, criteriaBuilder, root.get("resultado"), resultado);

            String textoNormalizado = normalizarOpcional(texto);
            if (textoNormalizado != null) {
                String patron = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                condiciones.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombreUsuario")), patron),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("correoUsuario")), patron),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ruta")), patron),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("detalle")), patron),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("recursoId")), patron)
                ));
            }
            return criteriaBuilder.and(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<AuditoriaOperacion> resultadoIgual(String resultado) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("resultado"), resultado);
    }

    private void agregarIgualSiExiste(
            List<jakarta.persistence.criteria.Predicate> condiciones,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            jakarta.persistence.criteria.Path<Object> campo,
            String valor
    ) {
        String normalizado = normalizarOpcional(valor);
        if (normalizado != null) {
            condiciones.add(criteriaBuilder.equal(campo, normalizado.toUpperCase(Locale.ROOT)));
        }
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        Preconditions.checkArgument(desde != null && hasta != null, "El rango de fechas es obligatorio.");
        Preconditions.checkArgument(!hasta.isBefore(desde), "La fecha final no puede ser anterior a la inicial.");
        Preconditions.checkArgument(!desde.plusDays(DIAS_MAXIMOS).isBefore(hasta),
                "El rango de auditoria no puede superar 366 dias.");
    }

    private String resolverNombreUsuario(AuditoriaRegistro registro) {
        if (registro.idUsuario() == null) {
            return "Sistema / usuario no autenticado";
        }
        return usuarioRepository.findByIdUsuarioAndEliminadoFalse(registro.idUsuario())
                .map(this::nombreCompleto)
                .orElseGet(() -> valorSeguro(registro.correoUsuario(), "Usuario #" + registro.idUsuario()));
    }

    private String nombreCompleto(Usuario usuario) {
        return (usuario.getNombre() + " " + usuario.getApellido()).trim();
    }

    private String valorSeguro(String valor, String predeterminado) {
        String normalizado = normalizarOpcional(valor);
        return normalizado == null ? predeterminado : normalizado;
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String recortar(String valor, int maximo) {
        String normalizado = normalizarOpcional(valor);
        return normalizado == null || normalizado.length() <= maximo
                ? normalizado
                : normalizado.substring(0, maximo);
    }
}
