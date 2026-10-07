import assert from "node:assert/strict";
import { test } from "node:test";
import { createApiClient } from "../client";
import { ApiError, type ApiErrorKind } from "../error";
import { createMockApiClient } from "../mock";
import { decodeApiResponse, decodeNoContent } from "../response";
import type { ApiClient, ErrorMapper, ResponseDecoder } from "../types";

const baseUrl = "https://api.example.invalid/custom/v2";
const identity: ResponseDecoder<unknown> = (body) => body;

// Validation-only DTO/fixtures. These are not proposed member or authority contracts.
interface FixtureDto { value: string | null; optional?: string }
const decodeFixture = decodeApiResponse<FixtureDto>((body) => {
  if (typeof body !== "object" || body === null || !("value" in body) ||
      (body.value !== null && typeof body.value !== "string")) {
    throw new Error("Required nullable value is missing or invalid.");
  }
  if ("optional" in body && typeof body.optional !== "string") {
    throw new Error("Optional string must be omitted, not null.");
  }
  return "optional" in body
    ? { value: body.value, optional: body.optional as string }
    : { value: body.value };
});

function hasKind(kind: ApiErrorKind, status?: number) {
  return (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.kind, kind);
    assert.equal(error.status, status);
    return true;
  };
}

test("base prefix, query, no implicit authentication/cache or redirect", async () => {
  let seen: Request | undefined;
  const client = createApiClient({ baseUrl, transport: async (request) => {
    seen = request;
    return Response.json({ data: { value: "ok" } });
  } });
  const result = await client.request("/fixture?cursor=a%2Fb", { decode: decodeFixture });
  assert.deepEqual(result, { data: { value: "ok" } });
  assert.ok(seen);
  assert.equal(seen.url, `${baseUrl}/fixture?cursor=a%2Fb`);
  assert.equal(seen.headers.get("Accept"), "application/json");
  assert.equal(seen.headers.has("Authorization"), false);
  assert.equal(seen.credentials, "omit");
  assert.equal(seen.cache, "no-store");
  assert.equal(seen.redirect, "error");
});

test("JSON request distinguishes nullable, omitted and empty array fields", async () => {
  const client = createApiClient({ baseUrl, transport: async (request) => {
    assert.equal(request.headers.get("Content-Type"), "application/json");
    assert.deepEqual(await request.json(), { nullable: null, items: [] });
    return new Response(null, { status: 204 });
  } });
  await client.request("fixture", {
    method: "PATCH", json: { nullable: null, optional: undefined, items: [] }, decode: decodeNoContent,
  });
});

test("raw FormData uses the browser multipart boundary", async () => {
  const body = new FormData();
  body.set("fixture", "test");
  const client = createApiClient({ baseUrl, transport: async (request) => {
    assert.match(request.headers.get("Content-Type") ?? "", /^multipart\/form-data; boundary=/);
    assert.equal((await request.formData()).get("fixture"), "test");
    return new Response(null, { status: 204 });
  } });
  await client.request("fixture", { method: "POST", body, decode: decodeNoContent });
});

test("session delivery is explicitly injected and refreshed on every request", async () => {
  let calls = 0;
  const client = createApiClient({ baseUrl,
    async prepareRequest(context) {
      assert.equal(context.method, "GET");
      // Mutating the hook's URL cannot redirect the actual request.
      context.url.host = "other.example.invalid";
      return { headers: { "X-Test-Session": String(++calls) }, credentials: "include" };
    },
    transport: async (request) => {
      assert.ok(request.url.startsWith(baseUrl));
      assert.equal(request.credentials, "include");
      assert.equal(request.headers.get("X-Test-Session"), String(calls));
      assert.equal(request.headers.get("X-Test-Request"), "kept");
      return Response.json(null);
    },
  });
  for (let i = 0; i < 2; i++) {
    await client.request("fixture", { headers: { "X-Test-Request": "kept" }, decode: identity });
  }
  assert.equal(calls, 2);
});

const statuses: [number, ApiErrorKind][] = [
  [400, "validation"], [401, "unauthorized"], [403, "forbidden"], [404, "unavailable"],
  [409, "conflict"], [413, "payload-too-large"], [415, "unsupported-media-type"],
  [422, "domain-validation"], [429, "rate-limit"], [500, "server"], [503, "server"], [418, "http"],
];
for (const [status, kind] of statuses) {
  test(`HTTP ${status} is ${kind}; preserves unconfirmed wire body`, async () => {
    const body = { arbitraryServerShape: ["reason", "region", "institution"] };
    const client = createApiClient({ baseUrl, transport: async () =>
      Response.json(body, { status, headers: { "Retry-After": "7" } }),
    });
    await assert.rejects(client.request("fixture", { decode: identity }), (error) => {
      hasKind(kind, status)(error);
      assert.ok(error instanceof ApiError);
      assert.deepEqual(error.body, body);
      assert.equal(error.headers?.get("Retry-After"), "7");
      assert.equal(error.code, undefined);
      return true;
    });
  });
}

test("an injected error mapper preserves distinct permission reasons", async () => {
  // Test-only shape/code strings, not #74 authority codes.
  const mapError: ErrorMapper = (body) => {
    assert.ok(typeof body === "object" && body !== null && "fixtureReason" in body);
    return { code: String(body.fixtureReason), details: body, traceId: "test-trace", message: "Test message" };
  };
  for (const fixtureReason of ["test.signup", "test.neighbor", "test.institution", "test.region"]) {
    const client = createMockApiClient({ mapError, routes: [{ method: "GET", path: "/fixture",
      respond: () => Response.json({ fixtureReason }, { status: 403 }),
    }] });
    await assert.rejects(client.request("fixture", { decode: identity }), (error) => {
      hasKind("forbidden", 403)(error);
      assert.ok(error instanceof ApiError);
      assert.equal(error.code, fixtureReason);
      assert.equal(error.traceId, "test-trace");
      assert.equal(error.message, "Test message");
      return true;
    });
  }
});

test("HTML/malformed HTTP error and failing error mapper retain HTTP classification", async () => {
  for (const status of [401, 403, 502]) {
    const client = createApiClient({ baseUrl,
      mapError: () => { throw new Error("Mapper failure"); },
      transport: async () => new Response("<html>proxy error</html>", { status }),
    });
    await assert.rejects(client.request("fixture", { decode: identity }), hasKind(
      status === 401 ? "unauthorized" : status === 403 ? "forbidden" : "server", status,
    ));
  }
});

test("malformed successful JSON and invalid DTOs fail at the shared response boundary", async () => {
  for (const raw of ["not JSON", "{}", '{"data":{}}', '{"data":{"value":null,"optional":null}}']) {
    const client = createApiClient({ baseUrl, transport: async () => new Response(raw) });
    await assert.rejects(client.request("fixture", { decode: decodeFixture }), hasKind("invalid-response", 200));
  }
});

test("JSON null, required nullable, omitted optional and no-content remain distinct", async () => {
  const client = createMockApiClient({ routes: [
    { method: "GET", path: "/null", respond: () => Response.json(null) },
    { method: "GET", path: "/nullable", respond: () => Response.json({ data: { value: null } }) },
    { method: "DELETE", path: "/empty", respond: () => new Response(null, { status: 204 }) },
    { method: "HEAD", path: "/head", respond: () => new Response(null) },
  ] });
  assert.equal(await client.request("null", { decode: identity }), null);
  const nullable = await client.request("nullable", { decode: decodeFixture });
  assert.deepEqual(nullable, { data: { value: null } });
  assert.equal("optional" in nullable.data, false);
  assert.equal(await client.request("empty", { method: "DELETE", decode: decodeNoContent }), undefined);
  assert.equal(await client.request("head", { method: "HEAD", decode: decodeNoContent }), undefined);
  await assert.rejects(client.request("null", { decode: decodeNoContent }), hasKind("invalid-response", 200));
});

test("API and Mock adapters use the same consumer/decoder for success, empty, pending and unavailable", async () => {
  const getValue = (client: ApiClient) => client.request("fixture", { decode: identity });
  const cases = [
    { body: { data: { value: "success" } }, status: 200 },
    { body: { data: [] }, status: 200 },
    { body: { data: { fixtureState: "pending" } }, status: 200 },
    { body: { arbitraryReason: "deleted" }, status: 404 },
  ];
  for (const fixture of cases) {
    const respond = () => Response.json(fixture.body, { status: fixture.status });
    const clients: ApiClient[] = [
      createApiClient({ baseUrl, transport: async () => respond() }),
      createMockApiClient({ routes: [{ method: "GET", path: "/fixture", respond }] }),
    ];
    for (const client of clients) {
      if (fixture.status === 404) await assert.rejects(getValue(client), hasKind("unavailable", 404));
      else assert.deepEqual(await getValue(client), fixture.body);
    }
  }
});

test("network failures are normalized without retrying a mutation", async () => {
  let calls = 0;
  const cause = new TypeError("Network failed");
  const client = createApiClient({ baseUrl, transport: async () => { calls++; throw cause; } });
  await assert.rejects(client.request("fixture", { method: "POST", json: {}, decode: identity }), (error) => {
    hasKind("network")(error);
    assert.ok(error instanceof ApiError);
    assert.equal(error.cause, cause);
    return true;
  });
  assert.equal(calls, 1);
});

test("body stream failures are network errors", async () => {
  const client = createApiClient({ baseUrl, transport: async () => new Response(new ReadableStream({
    start(controller) { controller.error(new TypeError("Body stream failed")); },
  })) });
  await assert.rejects(client.request("fixture", { decode: identity }), hasKind("network"));
});

test("pre-cancelled requests never prepare auth or call transport", async () => {
  const client = createApiClient({ baseUrl,
    prepareRequest: () => { assert.fail("Auth hook called"); },
    transport: async () => { assert.fail("Transport called"); },
  });
  await assert.rejects(client.request("fixture", {
    signal: AbortSignal.abort("custom reason"), decode: identity,
  }), hasKind("cancelled"));
});

test("cancellation during async session preparation never sends a request", async () => {
  const controller = new AbortController();
  const client = createApiClient({ baseUrl,
    async prepareRequest() { controller.abort("custom reason"); return {}; },
    transport: async () => { assert.fail("Transport called"); },
  });
  await assert.rejects(client.request("fixture", { signal: controller.signal, decode: identity }), hasKind("cancelled"));
});

test("an explicitly mapped auth preparation failure keeps its error category", async () => {
  const failure = new ApiError("unauthorized", "Test session required", { code: "test.session" });
  const client = createApiClient({ baseUrl,
    prepareRequest() { throw failure; },
    transport: async () => { assert.fail("Transport called"); },
  });
  await assert.rejects(client.request("fixture", { decode: identity }), (error) => error === failure);
});

test("Mock latency is asynchronous and cancellation releases the pending request", async () => {
  const controller = new AbortController();
  const client = createMockApiClient({ delayMs: 5000, routes: [{ method: "GET", path: "/fixture",
    respond: () => { assert.fail("Cancelled Mock handler called"); },
  }] });
  const pending = client.request("fixture", { signal: controller.signal, decode: identity });
  const assertion = assert.rejects(pending, hasKind("cancelled"));
  setTimeout(() => controller.abort(), 5);
  await assertion;
});

test("Mock cancellation also interrupts an asynchronous response handler", async () => {
  const controller = new AbortController();
  const started = Promise.withResolvers<void>();
  const reply = Promise.withResolvers<Response>();
  const client = createMockApiClient({ routes: [{ method: "GET", path: "/fixture",
    respond: () => { started.resolve(); return reply.promise; },
  }] });
  const pending = client.request("fixture", { signal: controller.signal, decode: identity });
  const assertion = assert.rejects(pending, hasKind("cancelled"));
  await started.promise;
  controller.abort("custom reason");
  await assertion;
  reply.reject(new Error("Late rejection is consumed"));
});

test("invalid configuration and endpoints cannot send credentials outside the base prefix", async () => {
  for (const url of ["", "not-url", "file:///tmp", "https://user:password@example.invalid/", `${baseUrl}?x=1`]) {
    assert.throws(() => createApiClient({ baseUrl: url }), hasKind("configuration"));
  }
  const client = createApiClient({ baseUrl, transport: async () => { assert.fail("Transport called"); } });
  for (const path of ["../outside", "https://external.example.invalid/", "//external.example.invalid/", "/fixture#fragment"]) {
    await assert.rejects(client.request(path, { decode: identity }), hasKind("configuration"));
  }
  await assert.rejects(client.request("fixture", { method: "POST", json: BigInt(1), decode: identity }), hasKind("configuration"));
});

test("unmatched Mock routes fail closed; production Mock activation is blocked", async () => {
  const client = createMockApiClient({ routes: [] });
  await assert.rejects(client.request("unconfigured", { decode: identity }), hasKind("configuration"));
  assert.throws(() => createMockApiClient({ routes: [], delayMs: -1 }), hasKind("configuration"));
  const previous = process.env.NODE_ENV;
  try {
    Reflect.set(process.env, "NODE_ENV", "production");
    assert.throws(() => createMockApiClient({ routes: [] }), hasKind("configuration"));
  } finally {
    if (previous === undefined) Reflect.deleteProperty(process.env, "NODE_ENV");
    else Reflect.set(process.env, "NODE_ENV", previous);
  }
});
