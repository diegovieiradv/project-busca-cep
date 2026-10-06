import Head from "next/head";
import CepLookup from "@/components/CepLookup";

export default function Home() {
  return (
    <>
      <Head>
        <title>Busca de CEP</title>
        <meta name="description" content="Consulta de endereço a partir de um CEP" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <main className="mx-auto max-w-md px-4 py-8">
        <h1 className="text-2xl font-semibold text-gray-900">Busca de CEP</h1>
        <p className="mt-2 text-sm text-gray-600">
          Informe um CEP com 8 dígitos (com ou sem hífen) para consultar o endereço.
        </p>
        <CepLookup />
      </main>
    </>
  );
}
