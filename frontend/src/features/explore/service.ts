import type { BoardFilters, BoardPage, ExploreContext, HomeContent } from "./model";

/** Adapter boundary for #67/#68. No endpoint, method, or DTO is assumed here. */
export interface ExploreService {
  getHome(context: ExploreContext): Promise<HomeContent>;
  getBoard(filters: BoardFilters, cursor?: string): Promise<BoardPage>;
}
