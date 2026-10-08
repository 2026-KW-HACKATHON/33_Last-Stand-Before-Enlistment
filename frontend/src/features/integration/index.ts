import { allPages, inputId } from "./wire";
import { decodeBoundaries, mapRegions, type MapCatalog } from "./map-geometry";
import type { ApiClient } from "../../lib/api/types";
import { createContentServices } from "./content-services";
import { createEditorServices } from "./editor-services";
import { createMemberServices } from "./member-services";

/** A creates this once per authenticated client/subject and injects the exported services. */
export function createFeatureServices(client: ApiClient, options: Parameters<typeof createContentServices>[1] = {}): FeatureServices {
  const content = createContentServices(client, options);
  const mapCatalogService = {
    async get(signal: AbortSignal): Promise<MapCatalog> {
      const [rows, source] = await Promise.all([
        allPages(client, "regions", signal),
        options.loadBoundaries ? options.loadBoundaries(signal) : fetch("/maps/administrative-dongs-20260701.geojson", { signal }).then(response => { if (!response.ok) throw new Error("Boundary download failed"); return response.json() as Promise<unknown>; }),
      ]);
      return mapRegions(rows, decodeBoundaries(source));
    },
  };
  return { mapCatalogService, mapRenderer: options.mapRenderer, ...content, ...createEditorServices(client), ...createMemberServices(client, content),
    createSharedServices(postId: string, token: string): FeatureServices {
      inputId(postId);
      if (!token || /[\r\n]/.test(token)) throw new Error("Invalid share context");
      const sharedClient: ApiClient = {
        request(path, request) {
          const target = path.split("?")[0];
          if (![`posts/${postId}`, `posts/${postId}/summary`, `posts/${postId}/comments`].includes(target) && !/^comments\/[1-9][0-9]*\/replies$/.test(target)) throw new Error("Shared request outside permitted feature scope");
          const headers = new Headers(request.headers); headers.set("X-Post-Share-Token", token);
          return client.request(path, { ...request, headers });
        },
      };
      return createFeatureServices(sharedClient, options);
    },
  };
}
export type FeatureServices = ReturnType<typeof createContentServices> & ReturnType<typeof createEditorServices> & ReturnType<typeof createMemberServices> & { mapCatalogService: { get(signal: AbortSignal): Promise<MapCatalog> }; mapRenderer?: import("../map/MapRenderer").MapRenderer; createSharedServices(postId: string, token: string): FeatureServices };
