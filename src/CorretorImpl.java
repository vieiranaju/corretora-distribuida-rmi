import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação da Corretora (Servidor RMI).
 * Gerencia os ativos e notifica os clientes registrados quando os preços mudam.
 *
 * O uso de "synchronized" garante que apenas um cliente por vez acesse
 * os recursos compartilhados (concorrência).
 */
public class CorretorImpl extends UnicastRemoteObject implements CorretorRemote {

    // Mapa de ativos: nome -> Ativo (acesso rápido por nome)
    private Map<String, Ativo> ativos;

    // Lista de clientes registrados para receber notificações (padrão Observer)
    private List<ClienteCallback> clientesRegistrados;

    public CorretorImpl() throws RemoteException {
        super();
        ativos = new HashMap<>();
        clientesRegistrados = new ArrayList<>();
    }

    /**
     * Cadastra um ativo na corretora.
     * Chamado diretamente pelo Servidor (não é método remoto).
     */
    public synchronized void cadastrarAtivo(String nome, double valorInicial) {
        String nomeUpper = nome.toUpperCase();
        ativos.put(nomeUpper, new Ativo(nomeUpper, valorInicial));
        System.out.println("[Servidor] Ativo cadastrado: " + nomeUpper + " = R$ " + valorInicial);
    }

    // ---------------------------------------------------------------
    // Implementação dos métodos da interface CorretorRemote
    // ---------------------------------------------------------------

    @Override
    public synchronized List<Ativo> listarAtivos() throws RemoteException {
        System.out.println("[Servidor] Cliente solicitou lista de ativos.");
        return new ArrayList<>(ativos.values());
    }

    @Override
    public synchronized double getValor(String nomeAtivo) throws RemoteException {
        Ativo ativo = ativos.get(nomeAtivo.toUpperCase());
        if (ativo == null) {
            System.out.println("[Servidor] Ativo não encontrado: " + nomeAtivo);
            return -1;
        }
        System.out.println("[Servidor] Consulta de preço: " + nomeAtivo + " = R$ " + ativo.getValor());
        return ativo.getValor();
    }

    @Override
    public synchronized void setValor(String nomeAtivo, double novoValor) throws RemoteException {
        Ativo ativo = ativos.get(nomeAtivo.toUpperCase());
        if (ativo == null) {
            System.out.println("[Servidor] Tentativa de atualizar ativo inexistente: " + nomeAtivo);
            return;
        }

        double valorAntigo = ativo.getValor();
        ativo.setValor(novoValor);

        System.out.println("[Servidor] Preço atualizado: " + nomeAtivo +
                " | R$ " + valorAntigo + " -> R$ " + novoValor);

        // Notifica todos os clientes registrados (padrão Observer)
        notificarTodos(nomeAtivo, novoValor);
    }

    @Override
    public synchronized void registrarCliente(ClienteCallback cliente) throws RemoteException {
        clientesRegistrados.add(cliente);
        System.out.println("[Servidor] Novo cliente registrado para notificações. Total: " + clientesRegistrados.size());
    }

    @Override
    public synchronized void desregistrarCliente(ClienteCallback cliente) throws RemoteException {
        clientesRegistrados.remove(cliente);
        System.out.println("[Servidor] Cliente desregistrado. Total: " + clientesRegistrados.size());
    }

    // ---------------------------------------------------------------
    // Método auxiliar: notifica todos os clientes (padrão Observer)
    // ---------------------------------------------------------------

    private void notificarTodos(String nomeAtivo, double novoValor) {
        List<ClienteCallback> clientesOffline = new ArrayList<>();

        for (ClienteCallback cliente : clientesRegistrados) {
            try {
                cliente.notificarMudancaPreco(nomeAtivo, novoValor);
            } catch (RemoteException e) {
                // Cliente caiu, marca para remover (tolerância a falhas básica)
                System.out.println("[Servidor] Cliente offline detectado, removendo da lista.");
                clientesOffline.add(cliente);
            }
        }

        // Remove clientes que não responderam
        clientesRegistrados.removeAll(clientesOffline);
    }
}
