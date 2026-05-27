package com.utp.RestoControl.Service;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Repository.CategoriaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;

    public List<CategoriaAlimento> listar() {
        return repository.findAll();
    }

    public CategoriaAlimento guardar(CategoriaAlimento categoria) {
        return repository.save(categoria);
    }
}