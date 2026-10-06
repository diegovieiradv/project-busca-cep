// Base do backend usada pelo cliente API. Definida antes dos imports para que
// `fetchCep` a utilize (a leitura é feita por chamada, veja `resolveApiBaseUrl`).
process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";

import { fetchCep } from "./api";
import { ApiError } from "./types";
import type { EnderecoResponse, ErrorResponse } from "./types";

const endereco: EnderecoResponse = {
  cep: "23565-100",
  logradouro: "Rua das Flores",
  complemento: "Bloco B",
  bairro: "Centro",
  localidade: "Seropédica",
  uf: "RJ",
};

const erroNotFound: ErrorResponse = {
  error: "NotFound",
  message: "CEP não encontrado",
  details: { cep: "00000-000" },
  timestamp: "2026-01-01T00:00:00Z",
  path: "/api/cep/00000-000",
};

/** Substitui o `fetch` global por um mock que devolve a resposta informada. */
function mockFetchResponse(response: { ok: boolean; status?: number; json: () => Promise<unknown> }): jest.Mock {
  const fetchMock = jest.fn().mockResolvedValue({
    ok: response.ok,
    status: response.status ?? 200,
    json: response.json,
  });
  global.fetch = fetchMock as unknown as typeof fetch;
  return fetchMock;
}

/** Captura o erro lançado por `fetchCep` (falha quando nada é lançado). */
async function captureError(cep: string): Promise<unknown> {
  try {
    await fetchCep(cep);
  } catch (error) {
    return error;
  }
  throw new Error("fetchCep deveria ter lançado um erro");
}

describe("fetchCep", () => {
  it("retorna o endereço quando a API responde 200", async () => {
    const fetchMock = mockFetchResponse({ ok: true, json: async () => endereco });

    await expect(fetchCep("23565-100")).resolves.toEqual(endereco);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/cep/23565-100");
  });

  it("lança ApiError com status e detalhes quando a API responde erro HTTP", async () => {
    mockFetchResponse({ ok: false, status: 404, json: async () => erroNotFound });

    const error = await captureError("00000-000");

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(404);
    expect(apiError.message).toBe("CEP não encontrado");
    expect(apiError.error).toBe("NotFound");
    expect(apiError.details).toEqual({ cep: "00000-000" });
  });

  it("lança ApiError com status quando o corpo de erro não é JSON válido", async () => {
    mockFetchResponse({
      ok: false,
      status: 502,
      json: async () => {
        throw new Error("não é JSON");
      },
    });

    const error = await captureError("23565-100");

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(502);
    expect(apiError.message).toContain("HTTP 502");
  });

  it("lança ApiError quando o fetch falha por rede", async () => {
    const fetchMock = jest.fn().mockRejectedValue(new TypeError("Failed to fetch"));
    global.fetch = fetchMock as unknown as typeof fetch;

    const error = await captureError("23565-100");

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(0);
    expect(apiError.error).toBe("NetworkError");
    expect(apiError.message).toBeTruthy();
  });
});
