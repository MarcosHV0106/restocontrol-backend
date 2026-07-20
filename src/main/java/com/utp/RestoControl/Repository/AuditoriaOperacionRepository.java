package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.AuditoriaOperacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AuditoriaOperacionRepository extends JpaRepository<AuditoriaOperacion, Long>,
        JpaSpecificationExecutor<AuditoriaOperacion> {

    @Query("select distinct a.modulo from AuditoriaOperacion a order by a.modulo")
    List<String> buscarModulos();

    @Query("select distinct a.accion from AuditoriaOperacion a order by a.accion")
    List<String> buscarAcciones();

    @Query("""
            select a.idUsuario, max(a.nombreUsuario), max(a.correoUsuario), max(a.rolUsuario)
            from AuditoriaOperacion a
            where a.idUsuario is not null
            group by a.idUsuario
            order by max(a.nombreUsuario)
            """)
    List<Object[]> buscarUsuarios();
}
