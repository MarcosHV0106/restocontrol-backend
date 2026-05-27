/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Alimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlimentoRepository
        extends JpaRepository<Alimento, Integer> {

}