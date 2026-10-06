import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CepLookup from "../CepLookup";
import { fetchCep } from "@/src/api";
import { ApiError } from "@/src/types";
import type { EnderecoResponse } from "@/src/types";

jest.mock("@/src/api", () => ({
  fetchCep: jest.fn(),
}));

const mockedFetchCep = jest.mocked(fetchCep);

const endereco: EnderecoResponse = {
  cep: "23565-100",
  logradouro: "Rua das Flores",
  complemento: "Bloco B",
  bairro: "Centro",
  localidade: "Seropédica",
  uf: "RJ",
};

describe("<CepLookup />", () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it("renderiza input e botão sem loading, erro ou resultado", () => {
    render(<CepLookup />);

    expect(screen.getByLabelText(/cep/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Buscar" })).toBeEnabled();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.queryByRole("region")).not.toBeInTheDocument();
    expect(mockedFetchCep).not.toHaveBeenCalled();
  });

  it("mostra loading durante a busca e depois exibe o resultado", async () => {
    let resolveLookup: (value: EnderecoResponse) => void = () => undefined;
    mockedFetchCep.mockImplementation(
      () =>
        new Promise<EnderecoResponse>((resolve) => {
          resolveLookup = resolve;
        }),
    );
    const user = userEvent.setup();
    render(<CepLookup />);

    await user.type(screen.getByLabelText(/cep/i), "23565-100");
    await user.click(screen.getByRole("button", { name: "Buscar" }));

    expect(mockedFetchCep).toHaveBeenCalledWith("23565-100");
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Buscando..." })).toBeDisabled();

    resolveLookup(endereco);

    const region = await screen.findByRole("region", { name: /resultado da busca/i });
    expect(region).toHaveTextContent("Rua das Flores");
    expect(region).toHaveTextContent("Centro");
    expect(region).toHaveTextContent("Seropédica/RJ");
    expect(region).toHaveTextContent("Bloco B");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Buscar" })).toBeEnabled();
  });

  it("exibe a mensagem de erro quando a consulta falha", async () => {
    mockedFetchCep.mockRejectedValue(new ApiError("CEP não encontrado", 404, { error: "NotFound" }));
    const user = userEvent.setup();
    render(<CepLookup />);

    await user.type(screen.getByLabelText(/cep/i), "00000-000");
    await user.click(screen.getByRole("button", { name: "Buscar" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("CEP não encontrado");
    expect(screen.queryByRole("region")).not.toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Buscar" })).toBeEnabled();
  });

  it("submete a busca ao pressionar Enter no campo", async () => {
    mockedFetchCep.mockResolvedValue(endereco);
    const user = userEvent.setup();
    render(<CepLookup />);

    await user.type(screen.getByLabelText(/cep/i), "23565100{Enter}");

    expect(await screen.findByRole("region", { name: /resultado da busca/i })).toBeInTheDocument();
    expect(mockedFetchCep).toHaveBeenCalledTimes(1);
    expect(mockedFetchCep).toHaveBeenCalledWith("23565100");
  });

  it("não consulta quando o campo está vazio", async () => {
    const user = userEvent.setup();
    render(<CepLookup />);

    await user.click(screen.getByRole("button", { name: "Buscar" }));

    expect(mockedFetchCep).not.toHaveBeenCalled();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });
});
