import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface de Callback do Cliente (padrão Observer).
 * O servidor usa essa interface para notificar os clientes sobre mudanças de preço
 * e sobre eventos do ciclo de vida do servidor (ex: encerramento).
 * O cliente também precisa ser um objeto remoto!
 */
public interface ClienteCallback extends Remote {

    /**
     * Chamado pelo servidor quando o preço de um ativo muda.
     *
     * @param nomeAtivo  nome do ativo que mudou
     * @param novoValor  novo valor do ativo
     */
    void notificarMudancaPreco(String nomeAtivo, double novoValor) throws RemoteException;

    /**
     * Chamado pelo servidor ANTES de encerrar, para avisar os clientes imediatamente.
     */
    void notificarEncerramento() throws RemoteException;
}
