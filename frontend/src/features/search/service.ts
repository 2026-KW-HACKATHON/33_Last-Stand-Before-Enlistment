import type { SearchFilters, SearchResult } from "./model";

/** Real adapter is added only after the search endpoint/DTO contract is agreed. */
export interface SearchService {
  search(filters: SearchFilters, signal?: AbortSignal): Promise<SearchResult>;
}
