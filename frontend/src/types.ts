/**
 * Contratos de dados compartilhados entre o cliente de API e a UI.
 *
 * Espelham os records Java do backend:
 * - `EnderecoResponse` (HTTP 200) em `com.diegovieira.buscacep.dto.EnderecoResponse`
 * - `ErrorResponse` (HTTP 400/404/502/500) em `com.diegovieira.buscacep.dto.ErrorResponse`
 */

/**
 * Resposta de sucesso de `GET /api/cep/{cep}`.
 *
 * O backend garante `cep` formatado como `#####-###`. `complemento` e
 * `logradouro` podem vir vazios, então a UI deve tratá-los defensivamente.
 */
export interface EnderecoResponse {
  /** CEP formatado como `#####-###`. */
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  /** Cidade. */
  localidade: string;
  /** Unidade federativa (sigla, ex.: `RJ`). */
  uf: string;
}

/**
 * Corpo de erro da API (HTTP 400/404/502/500).
 *
 * `error` é legível por máquina (`ValidationError`, `NotFound`, `BadGateway`,
 * `InternalServerError`) e `timestamp` é uma string ISO-8601 em UTC.
 */
export interface ErrorResponse {
  error: string;
  message: string;
  details: Record<string, string>;
  timestamp: string;
  path: string;
}

/**
 * Erro lançado pelo cliente de API.
 *
 * Cobre tanto falhas HTTP (resposta não-OK, com `status` preenchido e o
 * payload de `ErrorResponse` quando disponível) quanto falhas de rede ou de
 * parsing (nesse caso `status` é `0` e `error` identifica o tipo, ex.:
 * `NetworkError` / `InvalidBody`).
 */
export class ApiError extends Error {
  /** Status HTTP da resposta; `0` quando a falha foi de rede/parsing. */
  readonly status: number;
  /** Código legível por máquina vindo do `ErrorResponse`, quando existir. */
  readonly error?: string;
  /** Detalhes campo→mensagem vindo do `ErrorResponse`; vazio quando não houver. */
  readonly details: Record<string, string>;

  constructor(
    message: string,
    status: number,
    options: { error?: string; details?: Record<string, string>; cause?: unknown } = {},
  ) {
    super(message, options.cause !== undefined ? { cause: options.cause } : undefined);
    this.name = "ApiError";
    this.status = status;
    this.error = options.error;
    this.details = options.details ?? {};
  }
}
