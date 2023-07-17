module.exports = {
    //...
    devServer: {
      compress: true,
      public: '0.0.0.0:4200', // especificar o domínio ou IP e a porta que você deseja permitir
      disableHostCheck: true, // isso desativa a verificação do cabeçalho do host
    }
  };
  