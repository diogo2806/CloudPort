package br.com.cloudport.serviconaviosiderurgico.porta;

public interface CadastroNavioPorta {

    NavioCanonico buscarPorId(Long id);

    NavioCanonico buscarPorImo(String codigoImo);
}
