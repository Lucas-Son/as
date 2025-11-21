package com.fiap.esoa.salesmind.util;

import com.fiap.esoa.salesmind.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Gerenciador de transações com commit/rollback automático.
 */
public class TransactionManager {
    
    /**
     * Executa operações em uma transação e retorna um valor.
     * 
     * @param <T> Tipo de retorno
     * @param operation Função que recebe Connection e retorna resultado
     * @return Resultado da operação
     * @throws RuntimeException se a transação falhar
     */
    public static <T> T executeTransaction(Function<Connection, T> operation) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            T result = operation.apply(conn);
            
            conn.commit();
            return result;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaction rolled back due to error: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    System.err.println("Failed to rollback transaction: " + rollbackEx.getMessage());
                }
            }
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Executa operações em uma transação sem retorno (void).
     * 
     * @param operation Consumer que recebe Connection
     * @throws RuntimeException se a transação falhar
     */
    public static void executeTransactionVoid(Consumer<Connection> operation) {
        executeTransaction(conn -> {
            operation.accept(conn);
            return null;
        });
    }
    
    /**
     * Executa operações com tratamento customizado de exceções.
     * 
     * @param <T> Tipo de retorno
     * @param operation Função que recebe Connection e retorna resultado
     * @param onError Consumer para tratar erros
     * @return Resultado da operação ou null se ocorrer erro
     */
    public static <T> T executeTransactionWithHandler(
            Function<Connection, T> operation, 
            Consumer<Exception> onError) {
        
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            T result = operation.apply(conn);
            
            conn.commit();
            return result;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaction rolled back: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            
            if (onError != null) {
                onError.accept(e);
            }
            return null;
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
}
