package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.dto.EstatisticasEmpresaDTO;
import com.fiap.esoa.salesmind.repository.EmpresaRepository;
import com.fiap.esoa.salesmind.repository.GravacaoCallRepository;
import java.util.HashMap;
import java.util.Map;

public class DashboardService {

    private final EmpresaRepository empresaRepository;
    private final GravacaoCallRepository gravacaoRepository;

    public DashboardService(EmpresaRepository empresaRepository,
            GravacaoCallRepository gravacaoRepository) {
        this.empresaRepository = empresaRepository;
        this.gravacaoRepository = gravacaoRepository;
    }

    public Map<String, Object> getEmpresaDashboard(Long idEmpresa) {
        Map<String, Object> dashboard = new HashMap<>();
        
        EstatisticasEmpresaDTO stats = empresaRepository.getEstatisticas(idEmpresa);
        
        if (stats != null) {
            dashboard.put("totalGravacoes", stats.totalGravacoes());
            dashboard.put("totalClientes", stats.totalClientes());
            dashboard.put("taxaConversao", stats.taxaConversao());
        } else {
            dashboard.put("totalGravacoes", 0L);
            dashboard.put("totalClientes", 0L);
            dashboard.put("taxaConversao", 0.0);
        }
        
        return dashboard;
    }

    public Map<String, Object> getClienteDashboard(Long idCliente) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalGravacoes", gravacaoRepository.findByCliente(idCliente).size());
        dashboard.put("vendasFechadas", gravacaoRepository.countVendasFechadasByCliente(idCliente));
        return dashboard;
    }
}
