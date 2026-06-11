package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.ModalidadPedidoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModalidadPedidoService {

    private final ModalidadPedidoRepository repository;

    public List<ModalidadPedido> listar() {
        return repository.findByEliminadoFalse();
    }

    public ModalidadPedido guardar(
            ModalidadPedido modalidadPedido) {

        return repository.save(modalidadPedido);

    }

    public ModalidadPedido buscarPorId(
            Integer id) {

        return repository
                .findByIdModalidadPedidoAndEliminadoFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Modalidad de pedido no encontrada."
                        )
                );

    }

}
