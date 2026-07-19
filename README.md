# Projeto Busca CEP

Aplicacao Java para consulta de enderecos via CEP usando a API ViaCEP.

## Descricao

Aplicacao console que busca endereco por CEP na API ViaCEP, exibe no terminal e salva como arquivo JSON.

## Funcionalidades

- Consulta de CEP via API ViaCEP
- Exibicao do endereco completo
- Salvamento em arquivo JSON (formato pretty-print)

## Tecnologias

- **Linguagem:** Java 16+ (records)
- **HTTP Client:** `java.net.http.HttpClient` (Java 11+)
- **JSON:** Google Gson 2.13.2
- **API:** ViaCEP (viacep.com.br)

## Como Rodar

```bash
# Compile
javac -cp gson-2.13.2.jar -d out/production src/*.java

# Execute
java -cp "out/production:$HOME/Downloads/gson-2.13.2.jar" Principal
```

Ou abra no IntelliJ IDEA e execute `Principal.java`.

## Exemplo de Uso

```
Digite um cep: 23565-100
```

Saida:
```json
{
  "cep": "23565-100",
  "logradouro": "Estrada Jose Cid Fernandes",
  "bairro": "Santa Cruz",
  "uf": "RJ",
  "localidade": "Rio de Janeiro"
}
```

## Licenca

MIT License - Diego Vieira
