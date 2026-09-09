import { describe, it, expect, beforeAll } from "bun:test";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { binaryExists, runCli } from "./support/cli";
import { hasAuthSecrets, getE2EEnv } from "./support/env";
import { ensureSharedAuth, AuthRateLimitedError } from "./support/auth-setup";

describe("e2e: auth flow", () => {
  let configDir: string;
  let authSkipped = false;

  beforeAll(async () => {
    if (!binaryExists()) {
      throw new Error(
        "Binary not found. Run `bun run build:binary` before e2e tests.",
      );
    }
    if (!hasAuthSecrets()) return;

    try {
      configDir = await ensureSharedAuth();
    } catch (err) {
      if (err instanceof AuthRateLimitedError) {
        authSkipped = true;
        return;
      }
      throw err;
    }
  }, 60_000);

  it(
    "completes full magic-link login flow",
    () => {
      if (!hasAuthSecrets() || authSkipped) {
        console.log("Skipping: E2E auth not available");
        return;
      }

      const env = getE2EEnv();
      const authPath = join(configDir, "10x-cli", "auth.json");

      expect(existsSync(authPath)).toBe(true);

      const authData = JSON.parse(readFileSync(authPath, "utf8"));
      expect(authData.access_token).toBeTruthy();
      expect(authData.refresh_token).toBeTruthy();
      expect(authData.email).toBe(env.testEmail);
    },
    { timeout: 60_000 },
  );

  it(
    "persists session state across CLI invocations",
    () => {
      if (!hasAuthSecrets() || authSkipped) {
        console.log("Skipping: E2E auth not available");
        return;
      }

      const env = getE2EEnv();
      const authPath = join(configDir, "10x-cli", "auth.json");

      // Verify session was saved
      expect(existsSync(authPath)).toBe(true);
      const authData = JSON.parse(readFileSync(authPath, "utf8"));
      expect(authData.access_token).toBeTruthy();

      // Verify new CLI session can use saved credentials
      const result = runCli(["auth", "--status", "--json"], {
        env: { XDG_CONFIG_HOME: configDir, APPDATA: configDir },
      });

      expect(result.exitCode).toBe(0);
      const status = result.json<{ email: string; is_valid: boolean }>();
      expect(status.email).toBe(env.testEmail);
      expect(status.is_valid).toBe(true);
    },
    { timeout: 30_000 },
  );

  it(
    "allows authenticated CLI commands with saved session",
    () => {
      if (!hasAuthSecrets() || authSkipped) {
        console.log("Skipping: E2E auth not available");
        return;
      }

      // Run a command that requires authentication using the saved session
      const result = runCli(["list", "--json"], {
        env: { XDG_CONFIG_HOME: configDir, APPDATA: configDir },
      });

      expect(result.exitCode).toBe(0);
      expect(result.stdout.length).toBeGreaterThan(0);

      // Verify it returned valid JSON with catalog data
      const data = result.json();
      expect(data).toBeTruthy();
    },
    { timeout: 30_000 },
  );
});
