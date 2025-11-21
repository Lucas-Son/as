package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.model.Empresa;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para ClienteRepository com PostgreSQL
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClienteRepositoryTest {

    private static ClienteRepository repository;
    private static EmpresaRepository empresaRepository;

    private Long testClienteId;
    private Long testEmpresaId;

    @BeforeAll
    static void setupAll() {
        TestDataBuilder.configureTestDatabase();
        repository = new ClienteRepository();
        empresaRepository = new EmpresaRepository();
    }

    @BeforeEach
    void setup() {
        TestDataBuilder.cleanAllData();
        Empresa empresa = TestDataBuilder.createEmpresa(String.valueOf(System.currentTimeMillis()));
        testEmpresaId = empresa.getId();
    }

    @AfterEach
    void cleanup() {
        if (testClienteId != null) {
            try {
                repository.deleteById(testClienteId);
            } catch (Exception ignored) {
            }
            testClienteId = null;
        }
        if (testEmpresaId != null) {
            try {
                empresaRepository.deleteById(testEmpresaId);
            } catch (Exception ignored) {
            }
            testEmpresaId = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Inserir cliente e buscar por ID")
    void testInsertAndFindById() {
        Cliente cliente = TestDataBuilder.createCliente(testEmpresaId, "ins");
        testClienteId = cliente.getId();

        assertNotNull(cliente.getId());
        assertEquals(testEmpresaId, cliente.getIdEmpresa());
        assertEquals("Test Client ins", cliente.getNome());

        Optional<Cliente> found = repository.findById(testClienteId);
        assertTrue(found.isPresent());
        assertEquals("Test Client ins", found.get().getNome());
    }

    @Test
    @Order(2)
    @DisplayName("2. Atualizar cliente")
    void testUpdate() {
        Cliente cliente = TestDataBuilder.createCliente(testEmpresaId, "upd");
        testClienteId = cliente.getId();

        cliente.setNome("Updated Client Name");
        cliente.setTelefone("11988887777");
        repository.save(cliente);

        Optional<Cliente> updated = repository.findById(testClienteId);
        assertTrue(updated.isPresent());
        assertEquals("Updated Client Name", updated.get().getNome());
        assertEquals("11988887777", updated.get().getTelefone());
    }

    @Test
    @Order(3)
    @DisplayName("3. Restrição UNIQUE em cpf_cnpj")
    void testUniqueCpfCnpj() {
        Cliente cliente1 = TestDataBuilder.createCliente(testEmpresaId, "uniq1");
        testClienteId = cliente1.getId();

        Cliente cliente2 = new Cliente();
        cliente2.setIdEmpresa(testEmpresaId);
        cliente2.setNome("Duplicate CPF Client");
        cliente2.setCpfCnpj(cliente1.getCpfCnpj());
        cliente2.setTelefone("11999998888");

        assertThrows(RuntimeException.class, () -> {
            repository.save(cliente2);
        }, "Deve lançar exceção para email duplicado");
    }

    @Test
    @Order(5)
    @DisplayName("5. cpf_cnpj é opcional (pode ser NULL)")
    void testCpfCnpjOptional() {
        Cliente cliente = new Cliente();
        cliente.setIdEmpresa(testEmpresaId);
        cliente.setNome("No CPF Client");
        cliente.setTelefone("11999997777");

        Cliente saved = repository.save(cliente);
        testClienteId = saved.getId();
        
        assertNotNull(saved.getId(), "Deve salvar cliente sem cpf_cnpj");
        assertNull(saved.getCpfCnpj(), "cpf_cnpj deve permanecer null");
    }

    @Test
    @Order(6)
    @DisplayName("6. Buscar por empresa")
    void testFindByEmpresa() {
        Cliente c1 = TestDataBuilder.createCliente(testEmpresaId, "emp1");
        Cliente c2 = TestDataBuilder.createCliente(testEmpresaId, "emp2");
        testClienteId = c1.getId();

        List<Cliente> clientes = repository.findByEmpresa(testEmpresaId);

        assertTrue(clientes.size() >= 2);
        assertTrue(clientes.stream().anyMatch(c -> c.getId().equals(c1.getId())));
        assertTrue(clientes.stream().anyMatch(c -> c.getId().equals(c2.getId())));

        repository.deleteById(c2.getId());
    }

    @Test
    @Order(7)
    @DisplayName("7. Restrição de chave estrangeira para empresa")
    void testForeignKeyConstraint() {
        Cliente cliente = new Cliente();
        cliente.setIdEmpresa(999999L);
        cliente.setNome("Invalid FK Client");
        cliente.setCpfCnpj("99999999999");
        cliente.setTelefone("11999996666");

        assertThrows(RuntimeException.class, () -> {
            repository.save(cliente);
        }, "Deve lançar exceção para chave estrangeira inválida");
    }

    @Test
    @Order(8)
    @DisplayName("8. Restrição UNIQUE em telefone")
    void testUniqueTelefone() {
        Cliente c1 = new Cliente();
        c1.setIdEmpresa(testEmpresaId);
        c1.setNome("Cliente Tel 1");
        c1.setCpfCnpj("11111111111");
        c1.setTelefone("11999887766");
        c1 = repository.save(c1);
        testClienteId = c1.getId();

        Cliente c2 = new Cliente();
        c2.setIdEmpresa(testEmpresaId);
        c2.setNome("Cliente Tel 2");
        c2.setCpfCnpj("22222222222");
        c2.setTelefone("11999887766");

        assertThrows(RuntimeException.class, () -> {
            repository.save(c2);
        }, "Deve lançar exceção para telefone duplicado");
    }

    @Test
    @Order(9)
    @DisplayName("9. Restrição UNIQUE em email")
    void testUniqueEmail() {
        Cliente c1 = new Cliente();
        c1.setIdEmpresa(testEmpresaId);
        c1.setNome("Cliente Email 1");
        c1.setCpfCnpj("33333333333");
        c1.setEmail("unique@test.com");
        c1 = repository.save(c1);
        testClienteId = c1.getId();

        Cliente c2 = new Cliente();
        c2.setIdEmpresa(testEmpresaId);
        c2.setNome("Cliente Email 2");
        c2.setCpfCnpj("44444444444");
        c2.setEmail("unique@test.com");

        assertThrows(RuntimeException.class, () -> {
            repository.save(c2);
        }, "Deve lançar exceção para email duplicado");
    }

    @Test
    @Order(10)
    @DisplayName("10. Deletar cliente")
    void testDelete() {
        Cliente cliente = TestDataBuilder.createCliente(testEmpresaId, "del");
        Long id = cliente.getId();

        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        testClienteId = null;
    }

    @Test
    @Order(11)
    @DisplayName("11. Contar clientes por empresa")
    void testCountByEmpresa() {
        Cliente c1 = TestDataBuilder.createCliente(testEmpresaId, "count1");
        Cliente c2 = TestDataBuilder.createCliente(testEmpresaId, "count2");
        testClienteId = c1.getId();

        long count = repository.countByEmpresa(testEmpresaId);
        assertTrue(count >= 2, "Deve contar pelo menos 2 clientes");

        repository.deleteById(c2.getId());
    }

    @Test
    @Order(12)
    @DisplayName("12. Buscar por segmento")
    void testFindBySegmento() {
        Cliente c1 = TestDataBuilder.createCliente(testEmpresaId, "seg");
        c1.setSegmento("Tecnologia");
        repository.save(c1);
        testClienteId = c1.getId();

        List<Cliente> clientes = repository.findBySegmento("Tecnologia");
        assertTrue(clientes.size() > 0);
        assertTrue(clientes.stream().anyMatch(c -> c.getId().equals(testClienteId)));
    }

    @Test
    @Order(13)
    @DisplayName("13. Buscar por CPF/CNPJ")
    void testFindByCpfCnpj() {
        Cliente cliente = TestDataBuilder.createCliente(testEmpresaId, "cpf");
        testClienteId = cliente.getId();

        Optional<Cliente> found = repository.findByCpfCnpj(cliente.getCpfCnpj());
        assertTrue(found.isPresent());
        assertEquals(testClienteId, found.get().getId());
    }
}
