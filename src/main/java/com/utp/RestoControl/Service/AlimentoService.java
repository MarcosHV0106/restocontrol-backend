package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Repository.AlimentoRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlimentoService {

    private final AlimentoRepository repository;

    public List<Alimento> listar() {
        return repository.findAll();
    }

    public Alimento guardar(Alimento alimento) {
        return repository.save(alimento);
    }
}