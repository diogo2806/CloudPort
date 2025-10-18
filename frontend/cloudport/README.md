# Cloudport

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 16.1.5.

# CloudPort Front-end

## Configuração dinâmica da URL da API

A URL base consumida pelo front-end é definida em tempo de execução por meio do arquivo `src/assets/configuracao.json`. Esse arquivo é lido antes da inicialização do Angular e disponibilizado para todos os serviços através do `ConfiguracaoAplicacaoService`.

1. Copie o arquivo de exemplo:
   ```bash
   cp src/assets/configuracao.exemplo.json src/assets/configuracao.json
   ```
2. Edite o arquivo `src/assets/configuracao.json` e informe o valor desejado para `baseApiUrl`, por exemplo:
   ```json
   {
     "baseApiUrl": "http://localhost:8080"
   }
   ```
3. Inicie a aplicação normalmente com `npm start` ou `ng serve`.

Caso o arquivo não exista, esteja inacessível ou não contenha o campo `baseApiUrl`, a aplicação interromperá o carregamento e exibirá uma mensagem em português orientando sobre o problema.

## Servidor de desenvolvimento

Execute `ng serve` para iniciar o servidor de desenvolvimento. Acesse `http://localhost:4200/`. A aplicação será recarregada automaticamente sempre que qualquer arquivo fonte for alterado.

## Geração de código

Execute `ng generate component component-name` para criar um novo componente. Também é possível usar `ng generate directive|pipe|service|class|guard|interface|enum|module` conforme a necessidade.

## Build

Execute `ng build` para gerar o build do projeto. Os artefatos ficarão armazenados na pasta `dist/`.

## Testes unitários

Execute `ng test` para rodar os testes unitários com o [Karma](https://karma-runner.github.io).

## Testes fim a fim

Execute `ng e2e` para rodar os testes fim a fim na plataforma desejada. Para utilizar esse comando é necessário adicionar previamente um pacote que implemente os recursos de testes e2e.

## Ajuda adicional

Para mais informações sobre o Angular CLI utilize `ng help` ou consulte a [documentação oficial do Angular CLI](https://angular.io/cli).
