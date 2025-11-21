package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.model.FeedbackIA;
import com.fiap.esoa.salesmind.repository.FeedbackIARepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class FeedbackIAService {

    private final FeedbackIARepository repository;

    public FeedbackIAService(FeedbackIARepository repository) {
        this.repository = repository;
    }

    public FeedbackIA save(FeedbackIA feedback) {
        return repository.save(feedback);
    }

    public FeedbackIA saveWithConnection(Connection conn, FeedbackIA feedback) throws SQLException {
        return repository.saveWithConnection(conn, feedback);
    }

    public Optional<FeedbackIA> findById(Long id) {
        return repository.findById(id);
    }

    public List<FeedbackIA> findAll() {
        return repository.findAll();
    }

    public Optional<FeedbackIA> findByGravacao(Long idGravacao) {
        return repository.findByGravacao(idGravacao);
    }

    public List<FeedbackIA> findByEmpresa(Long idEmpresa) {
        return repository.findByEmpresa(idEmpresa);
    }
}
