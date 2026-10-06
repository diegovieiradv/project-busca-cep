import { ApiError } from "./types";
import type { EnderecoResponse, ErrorResponse } from "./types";

/** Caminho relativo ao origin do backend (mesmo host). */
const API_BASE_PATH = "/api/cep";

/**
 * Consulta o endereço correspondente a um CEP.
 *
 * Aceita o CEP com ou sem hífen (`23565-100` / `23565100`): a normalização
 * é responsabilidade do backend.
 *
 * @param cep CEP a ser consultado
 * @return o payload de sucesso da API (HTTP 200)
 * @throws {ApiError} em falha de rede (`status === 0`), resposta não-OK ou
 * corpo de resposta inválido — carregando `status`, `error`, `message` e
 * `details` quando o backend os enviar.
 */
export async function fetchCep(cep: string): Promise<EnderecoResponse> {
  const url = `${API_BASE_PATH}/${encodeURIComponent(cep.trim())}`;

  let response: Response;
  try {
    response = await fetch(url);
  } catch (cause) {
    throw new ApiError("Não foi possível conectar à API de CEP. Verifique sua conexão.", 0, {
      error: "NetworkError",
      cause,
    });
  }

  if (!response.ok) {
    const body = await readErrorBody(response);
    if (body) {
      throw new ApiError(body.message, response.status, { error: body.error, details: body.details });
    }
    throw new ApiError(`Falha na consulta do CEP (HTTP ${response.status}).`, response.status, {
      error: "UnexpectedStatus",
    });
  }

  try {
    return (await response.json()) as EnderecoResponse;
  } catch (cause) {
    throw new ApiError("Resposta inválida recebida da API de CEP.", response.status, {
      error: "InvalidBody",
      cause,
    });
  }
}

/** Lê o corpo de erro como `ErrorResponse`; devolve `null` se não for JSON válido. */
async function readErrorBody(response: Response): Promise<ErrorResponse | null> {
  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    return null;
  }
  return isErrorResponse(payload) ? payload : null;
}

function isErrorResponse(payload: unknown): payload is ErrorResponse {
  if (typeof payload !== "object" || payload === null) {
    return false;
  }
  const candidate = payload as Partial<ErrorResponse>;
  return typeof candidate.error === "string" && typeof candidate.message === "string";
}
