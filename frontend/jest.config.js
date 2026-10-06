/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  // Builds do Next.js não contêm testes.
  testPathIgnorePatterns: ["/node_modules/", "/.next/"],
  // Reproduz o alias `@/*` do tsconfig.json no Jest.
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/$1",
  },
  transform: {
    "^.+\\.tsx?$": [
      "ts-jest",
      {
        // O tsconfig do Next.js usa ESM (`module: esnext`); o Jest executa CJS.
        tsconfig: {
          module: "commonjs",
          moduleResolution: "node",
        },
      },
    ],
  },
};
