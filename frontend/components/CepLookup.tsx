import { useState } from "react";
import type { FormEvent, KeyboardEvent } from "react";
import { fetchCep } from "@/src/api";
import { ApiError } from "@/src/types";
import type { EnderecoResponse } from "@/src/types";

export interface CepLookupProps {
  /** CEP pré-preenchido no campo de busca. */
  initialCep?: string;
}

/** Estado do formulário de busca. */
export interface CepLookupState {
  cep: string;
  loading: boolean;
  error: string | null;
  result: EnderecoResponse | null;
}

/** Traduz a exceção capturada em uma mensagem exibível ao usuário. */
function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "Erro inesperado ao consultar o CEP. Tente novamente.";
}

/**
 * Formulário de busca de CEP: input + botão, com estados de carregamento,
 * erro e resultado.
 *
 * Aceita o CEP com ou sem hífen (o backend normaliza). A consulta é feita
 * via `fetchCep`.
 */
export default function CepLookup({ initialCep = "" }: CepLookupProps) {
  const [state, setState] = useState<CepLookupState>({
    cep: initialCep,
    loading: false,
    error: null,
    result: null,
  });

  const handleSubmit = async (event?: FormEvent<HTMLFormElement>): Promise<void> => {
    event?.preventDefault();

    const cep = state.cep.trim();
    if (state.loading || cep.length === 0) {
      return;
    }

    setState((current) => ({ ...current, loading: true, error: null, result: null }));
    try {
      const endereco = await fetchCep(cep);
      setState((current) => ({ ...current, result: endereco }));
    } catch (error) {
      setState((current) => ({ ...current, error: describeError(error) }));
    } finally {
      setState((current) => ({ ...current, loading: false }));
    }
  };

  // Enter no input submete o formulário; preventDefault cancela a submissão
  // implícita do navegador para não disparar duas buscas.
  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>): void => {
    if (event.key === "Enter") {
      event.preventDefault();
      void handleSubmit();
    }
  };

  const { cep, loading, error, result } = state;

  return (
    <section className="mx-auto mt-6 w-full max-w-md rounded border border-gray-200 bg-white p-4 shadow">
      <form
        onSubmit={(event) => void handleSubmit(event)}
        className="flex flex-col gap-3 sm:flex-row sm:items-end"
      >
        <div className="flex-1">
          <label htmlFor="cep" className="mb-1 block text-sm font-medium text-gray-700">
            CEP
          </label>
          <input
            id="cep"
            name="cep"
            type="text"
            placeholder="23565-100 ou 23565100"
            value={cep}
            onChange={(event) => setState((current) => ({ ...current, cep: event.target.value }))}
            onKeyDown={handleKeyDown}
            disabled={loading}
            autoComplete="postal-code"
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100"
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? "Buscando..." : "Buscar"}
        </button>
      </form>

      {loading && (
        <p role="status" className="mt-4 text-sm text-gray-600">
          Buscando CEP...
        </p>
      )}

      {error && (
        <p
          role="alert"
          className="mt-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700"
        >
          {error}
        </p>
      )}

      {result && (
        <div
          role="region"
          aria-label="Resultado da busca"
          className="mt-4 space-y-1 rounded border border-gray-200 bg-gray-50 p-3 text-sm text-gray-900"
        >
          <p>
            <span className="font-medium">CEP:</span> {result.cep}
          </p>
          <p>
            <span className="font-medium">Logradouro:</span> {result.logradouro}
          </p>
          <p>
            <span className="font-medium">Bairro:</span> {result.bairro}
          </p>
          <p>
            <span className="font-medium">Cidade/UF:</span> {result.localidade}/{result.uf}
          </p>
          {result.complemento && (
            <p>
              <span className="font-medium">Complemento:</span> {result.complemento}
            </p>
          )}
        </div>
      )}
    </section>
  );
}
