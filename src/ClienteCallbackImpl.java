import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementação do Callback do Cliente.
 *
 * Mantém uma referência compartilhada (AtomicReference) ao objeto remoto do
 * servidor, e um flag (AtomicBoolean) para indicar se reconexão está em curso.
 *
 * Quando o servidor avisa que está encerrando (notificarEncerramento),
 * this class dispara automaticamente uma thread de reconexão em background que:
 *  1. Fica tentando se conectar ao servidor indefinidamente (a cada 3s).
 *  2. Quando conseguir, re-registra o callback e atualiza a referência.
 *  3. Imprime uma mensagem visual de sucesso no console.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    private static final int INTERVALO_RECONEXAO_MS = 3000;

    private final String nomeCliente;
    private final AtomicReference<CorretorRemote> corretorRef;
    private final AtomicBoolean reconectando;

    public ClienteCallbackImpl(String nomeCliente,
                               AtomicReference<CorretorRemote> corretorRef,
                               AtomicBoolean reconectando) throws RemoteException {
        super();
        this.nomeCliente = nomeCliente;
        this.corretorRef = corretorRef;
        this.reconectando = reconectando;
    }

    /** Chamado pelo servidor quando o preço de um ativo muda. */
    @Override
    public void notificarMudancaPreco(String nomeAtivo, double novoValor) throws RemoteException {
        System.out.println("\n>>> [NOTIFICAÇÃO] " + nomeAtivo
                + " mudou para R$ " + String.format("%.2f", novoValor));
        System.out.print("Escolha uma opção: ");
    }

    /** Chamado pelo servidor ANTES de encerrar — avisa imediatamente e dispara reconexão. */
    @Override
    public void notificarEncerramento() throws RemoteException {
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║  ⚠  SERVIDOR ENCERRADO!                 ║");
        System.out.println("║  Tentando reconectar automaticamente... ║");
        System.out.println("╚══════════════════════════════════════════╝");
        iniciarReconexaoBackground();
    }

    /**
     * Inicia a thread de reconexão em background.
     * Usa compareAndSet para garantir que só uma thread de reconexão roda por vez.
     */
    public void iniciarReconexaoBackground() {
        if (reconectando.compareAndSet(false, true)) {
            corretorRef.set(null); // invalida a referência atual
            Thread t = new Thread(this::loopReconexao, "Thread-Reconexao");
            t.setDaemon(true);    // morre junto com o processo principal
            t.start();
        }
    }

    /** Loop de reconexão que roda em background até conseguir conectar. */
    private void loopReconexao() {
        try {
            while (true) {
                System.out.println("Servidor indisponível. Tentando novamente em "
                        + (INTERVALO_RECONEXAO_MS / 1000) + "s...");
                try {
                    Thread.sleep(INTERVALO_RECONEXAO_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    CorretorRemote novoCorretor = (CorretorRemote) registry.lookup("CorretorService");
                    novoCorretor.registrarCliente(this); // re-registra callback
                    corretorRef.set(novoCorretor);       // atualiza ref compartilhada

                    System.out.println("\n╔══════════════════════════════════════════╗");
                    System.out.println("║  ✔  RECONECTADO AO SERVIDOR!            ║");
                    System.out.println("║  Você pode continuar operando.          ║");
                    System.out.println("╚══════════════════════════════════════════╝");
                    System.out.println("\n========== CORRETORA RMI ==========");
                    System.out.println("1. Listar ativos");
                    System.out.println("2. Consultar preço de um ativo");
                    System.out.println("3. Comprar ativo (preço sobe 5%)");
                    System.out.println("4. Vender ativo (preço cai 5%)");
                    System.out.println("0. Sair");
                    System.out.print("Escolha uma opção: ");
                    return; // reconexão concluída
                } catch (Exception e) {
                    // servidor ainda fora, continua tentando
                }
            }
        } finally {
            reconectando.set(false);
        }
    }

    public String getNomeCliente() {
        return nomeCliente;
    }
}
