package br.com.senai.sublime_app.provider.repository;

import java.math.BigInteger;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.provider.model.ProviderModel;

public  interface ProviderRepository extends JpaRepository<ProviderModel, BigInteger> {

    
}
