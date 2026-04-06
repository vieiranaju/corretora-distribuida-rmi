import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente da Corretora RMI.
 *
 * Funcionalidades:
 *  1. Listar ativos disponíveis
 *  2. Consultar preço de um ativo
 *  3. Comprar ativo (aumenta o preço)
 *  4. Vender ativo (diminui o preço)
 *  0. Sair
 *
 * O cliente registra um callback para receber notificações em tempo real
 * quando qualquer outro cliente alterar o preço de um ativo.
 */
public class Cliente {

    // Percentual de variação ao comprar/vender
    private static final double VARIACAO_PERCENTUAL = 0.05; // 5%

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Digite seu nome: ");
        String nomeCliente = scanner.nextLine();

        CorretorRemote corretor = null;
        ClienteCallbackImpl meuCallback = null;

        // ---------------------------------------------------------------
        // Conecta ao servidor (com tolerância a falhas: tenta 3 vezes)
        // ---------------------------------------------------------------
        corretor = conectarComRetentativa(nomeCliente);
        if (corretor == null) {
            System.out.println("Não foi possível conectar ao servidor. Encerrando.");
            return;
        }

        // ---------------------------------------------------------------
        // Registra o callback para receber notificações do servidor
        // ---------------------------------------------------------------
        try {
            meuCallback = new ClienteCallbackImpl(nomeCliente);
            corretor.registrarCliente(meuCallback);
            System.out.println("[" + nomeCliente + "] Registrado para receber notificações em tempo real!");
        } catch (Exception e) {
            System.out.println("Erro ao registrar callback: " + e.getMessage());
        }

        // ---------------------------------------------------------------
        // Menu principal
        // ---------------------------------------------------------------
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n========== CORRETORA RMI ==========");
            System.out.println("1. Listar ativos");
            System.out.println("2. Consultar preço de um ativo");
            System.out.println("3. Comprar ativo (preço sobe 5%)");
            System.out.println("4. Vender ativo (preço cai 5%)");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            String opcao = scanner.nextLine().trim();

            try {
                switch (opcao) {
                    case "1":
                        listarAtivos(corretor);
                        break;

                    case "2":
                        System.out.print("Nome do ativo: ");
                        String nomeConsulta = scanner.nextLine().trim().toUpperCase();
                        consultarPreco(corretor, nomeConsulta);
                        break;

                    case "3":
                        System.out.print("Nome do ativo para COMPRAR: ");
                        String nomeCompra = scanner.nextLine().trim().toUpperCase();
                        comprarAtivo(corretor, nomeCompra);
                        break;

                    case "4":
                        System.out.print("Nome do ativo para VENDER: ");
                        String nomeVenda = scanner.nextLine().trim().toUpperCase();
                        venderAtivo(corretor, nomeVenda);
                        break;

                    case "0":
                        rodando = false;
                        break;

                    default:
                        System.out.println("Opção inválida!");
                }

            } catch (Exception e) {
                System.out.println("[ERRO] " + e.getMessage());

                // Tolerância a falhas: tenta reconectar se o servidor caiu
                System.out.println("Tentando reconectar ao servidor...");
                corretor = conectarComRetentativa(nomeCliente);
                if (corretor == null) {
                    System.out.println("Servidor indisponível. Encerrando.");
                    rodando = false;
                }
            }
        }

        // ---------------------------------------------------------------
        // Ao sair, desregistra o callback
        // ---------------------------------------------------------------
        try {
            if (corretor != null && meuCallback != null) {
                corretor.desregistrarCliente(meuCallback);
            }
        } catch (Exception e) {
            // ignora erros ao desconectar
        }

        System.out.println("Até logo, " + nomeCliente + "!");
        scanner.close();
    }

    // ---------------------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------------------

    private static void listarAtivos(CorretorRemote corretor) throws Exception {
        List<Ativo> ativos = corretor.listarAtivos();
        System.out.println("\n--- Ativos disponíveis ---");
        for (Ativo a : ativos) {
            System.out.println("  " + a);
        }
    }

    private static void consultarPreco(CorretorRemote corretor, String nome) throws Exception {
        double valor = corretor.getValor(nome);
        if (valor < 0) {
            System.out.println("Ativo '" + nome + "' não encontrado.");
        } else {
            System.out.printf("Preço de %s: R$ %.2f%n", nome, valor);
        }
    }

    private static void comprarAtivo(CorretorRemote corretor, String nome) throws Exception {
        double valorAtual = corretor.getValor(nome);
        if (valorAtual < 0) {
            System.out.println("Ativo '" + nome + "' não encontrado.");
            return;
        }
        // Comprar faz o preço subir
        double novoValor = valorAtual * (1 + VARIACAO_PERCENTUAL);
        corretor.setValor(nome, novoValor);
        System.out.printf("COMPRA realizada! %s: R$ %.2f -> R$ %.2f%n", nome, valorAtual, novoValor);
    }

    private static void venderAtivo(CorretorRemote corretor, String nome) throws Exception {
        double valorAtual = corretor.getValor(nome);
        if (valorAtual < 0) {
            System.out.println("Ativo '" + nome + "' não encontrado.");
            return;
        }
        // Vender faz o preço cair
        double novoValor = valorAtual * (1 - VARIACAO_PERCENTUAL);
        corretor.setValor(nome, novoValor);
        System.out.printf("VENDA realizada! %s: R$ %.2f -> R$ %.2f%n", nome, valorAtual, novoValor);
    }

    /**
     * Tenta conectar ao servidor até 3 vezes antes de desistir.
     * Implementa tolerância a falhas básica.
     */
    private static CorretorRemote conectarComRetentativa(String nomeCliente) {
        int tentativas = 3;
        while (tentativas > 0) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                CorretorRemote corretor = (CorretorRemote) registry.lookup("CorretorService");
                System.out.println("[" + nomeCliente + "] Conectado ao servidor com sucesso!");
                return corretor;
            } catch (Exception e) {
                tentativas--;
                System.out.println("Falha na conexão. Tentativas restantes: " + tentativas);
                if (tentativas > 0) {
                    try {
                        Thread.sleep(2000); // aguarda 2s antes de tentar novamente
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return null;
    }
}
