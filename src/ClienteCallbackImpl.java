import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementação do Callback do Cliente.
 * Esta classe fica "rodando" no lado do cliente e é chamada remotamente pelo servidor.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    private String nomeCliente;

    public ClienteCallbackImpl(String nomeCliente) throws RemoteException {
        super(); // exporta este objeto como objeto remoto
        this.nomeCliente = nomeCliente;
    }

    /**
     * Este método é chamado PELO SERVIDOR quando um preço muda.
     * É a essência do padrão Observer no RMI.
     */
    @Override
    public void notificarMudancaPreco(String nomeAtivo, double novoValor) throws RemoteException {
        System.out.println("\n>>> [NOTIFICAÇÃO] " + nomeAtivo + " mudou para R$ " + String.format("%.2f", novoValor));
        System.out.print("Escolha uma opção: ");
    }

    public String getNomeCliente() {
        return nomeCliente;
    }
}
