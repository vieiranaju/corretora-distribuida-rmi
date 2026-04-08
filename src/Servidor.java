import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Ponto de entrada do Servidor RMI da Corretora.
 * Cria o registro RMI e publica o objeto remoto.
 *
 * Ao encerrar (Ctrl+C ou qualquer shutdown), notifica todos os
 * clientes conectados imediatamente via callback.
 */
public class Servidor {

    public static void main(String[] args) {
        try {
            // 1. Cria a implementação da Corretora
            CorretorImpl corretor = new CorretorImpl();

            // 2. Cadastra os ativos disponíveis na corretora (gerenciado pelo servidor)
            corretor.cadastrarAtivo("BTC",   350000.00);
            corretor.cadastrarAtivo("ETH",   18500.00);
            corretor.cadastrarAtivo("SOL",   850.00);
            corretor.cadastrarAtivo("PETR4", 38.50);
            corretor.cadastrarAtivo("VALE3", 65.20);

            // 3. Cria o registro RMI na porta 1099 (porta padrão)
            Registry registry = LocateRegistry.createRegistry(1099);

            // 4. Publica o objeto com o nome "CorretorService"
            registry.rebind("CorretorService", corretor);

            // 5. ShutdownHook: avisa TODOS os clientes antes de encerrar
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Servidor] Encerrando... notificando clientes conectados.");
                corretor.notificarClientesEncerramento();
                System.out.println("[Servidor] Servidor encerrado.");
            }));

            System.out.println("===========================================");
            System.out.println("  Servidor da Corretora RMI iniciado!     ");
            System.out.println("  Porta: 1099                             ");
            System.out.println("  Serviço: CorretorService                ");
            System.out.println("  (Ctrl+C para encerrar)                  ");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("[ERRO no Servidor] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
